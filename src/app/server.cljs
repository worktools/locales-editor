
(ns app.server
  (:require [app.schema :as schema]
            [app.updater :refer [updater]]
            [cljs.reader :refer [read-string]]
            [cumulo-reel.core :refer [reel-reducer refresh-reel reel-schema]]
            ["fs" :as fs]
            ["shortid" :as shortid]
            ["child_process" :as cp]
            ["path" :as path]
            [app.config :as config]
            [clojure.string :as string]
            [favored-edn.core :refer [write-edn]]
            ["latest-version" :as latest-version]
            ["chalk" :as chalk]
            ["axios" :as axios]
            ["md5" :as md5]
            ["gaze" :as gaze]
            [clojure.set :refer [intersection difference]]
            [cumulo-util.core :refer [id! repeat! unix-time! delay!]]
            [ws-edn.server :refer [wss-serve! wss-send! wss-each!]]
            [cumulo-util.file :refer [get-backup-path! merge-local-edn!]]
            [recollect.diff :refer [diff-twig]]
            [recollect.twig :refer [render-twig]]
            [app.twig.container :refer [twig-container]]
            [app.locales :as locales]
            [app.config-file :refer [validate!]]
            [cljs.core.async :refer [go <!]]
            ["dotenv" :as dotnet])
  (:require-macros [clojure.core.strint :refer [<<]]))

(defonce *client-caches (atom {}))

(def storage-file (path/join (js/process.cwd) "locales.edn"))

(defonce initial-db
  (let [storage (merge-local-edn!
                 schema/database
                 storage-file
                 (fn [found?]
                   (if found? (println "Found local EDN data") (println "Found no data"))))
        schema-version (get-in storage [:schema :version])
        cli-version (get-in schema/database [:schema :version])]
    (when (not= schema-version cli-version)
      (println
       (<<
        "Schema version(~{schema-version}) does not match version of cli(~{cli-version}). Existing!"))
      (.exit js/process 1))
    (assoc storage :saved-locales (:locales storage))))

(defonce *reel (atom (merge reel-schema {:base initial-db, :db initial-db})))

(defonce *reader-reel (atom @*reel))

(defonce *storage-md5 (atom nil))

(defn check-version! []
  (let [pkg (.parse js/JSON (fs/readFileSync (path/join js/__dirname "../package.json")))
        version (.-version pkg)
        pkg-name (.-name pkg)]
    (-> (latest-version (.-name pkg))
        (.then
         (fn [npm-version]
           (if (= npm-version version)
             (println "Running latest version" version)
             (println
              (.yellow
               chalk
               (<<
                "New version ~{npm-version} available, current one is ~{version} . Please upgrade!\n\nyarn global add ~{pkg-name}\n")))))))))

(defn write-mildly! [file-path content]
  (let [dir (path/dirname file-path)
        filename (path/basename file-path)
        temp-name (str
                   (or js/process.env.TMP_FOLDER "/tmp/")
                   (.now js/Date)
                   "-"
                   (.random js/Math)
                   "-"
                   filename)
        do-write! (fn []
                    (fs/writeFileSync temp-name content)
                    (fs/renameSync temp-name file-path)
                    (println "Write to file:" file-path))]
    (if (fs/existsSync file-path)
      (let [old-content (fs/readFileSync file-path "utf8")]
        (if (not= content old-content)
          (do-write!)
          (comment println "same file, skipping:" file-path)))
      (do
       (when (and (not= "." dir) (not (fs/existsSync dir)))
         (fs/mkdirSync dir (clj->js {:recursive true})))
       (do-write!)))))

(defn persist-db! []
  (let [file-content (write-edn
                      (-> (:db @*reel) (assoc :sessions {}) (dissoc :saved-locales))
                      {:indent 2})
        now (unix-time!)]
    (reset! *storage-md5 (md5 file-content))
    (write-mildly! storage-file file-content)
    (comment write-mildly! (get-backup-path!) file-content)
    (println "Saved file in" storage-file)))

(defn dispatch! [op op-data sid]
  (let [op-id (id!), op-time (unix-time!), db (:db @*reel)]
    (when config/dev? (println "Dispatch!" (str op) (subs (pr-str op-data) 0 140)))
    (try
     (cond
       (= op :effect/persist) (persist-db!)
       (= op :effect/codegen)
         (do
          (locales/show-changes! (:locales db) (:saved-locales db))
          (locales/generate-files! (:db @*reel))
          (persist-db!)
          (dispatch! :locale/mark-saved nil sid))
       (= op :effect/translate)
         (go
          (let [result (<! (locales/chan-translate-sentence (last op-data) (:settings db)))]
            (dispatch! :session/store-translation {:key (first op-data), :text result} sid)))
       :else
         (let [new-reel (reel-reducer @*reel updater op op-data sid op-id op-time)]
           (reset! *reel new-reel)))
     (catch js/Error error (.error js/console error)))))

(defn on-exit! [code]
  (persist-db!)
  (println "exit code is:" (pr-str code))
  (.exit js/process))

(defn on-file-change! [filepath]
  (let [content (fs/readFileSync filepath "utf8"), new-md5 (md5 content)]
    (when (not= new-md5 @*storage-md5)
      (println "File changed by comparing md5")
      (reset! *storage-md5 new-md5)
      (dispatch! :locale/checkout (:locales (read-string content)) nil))))

(defn sync-clients! [reel]
  (wss-each!
   (fn [sid socket]
     (let [db (:db reel)
           records (:records reel)
           session (get-in db [:sessions sid])
           old-store (or (get @*client-caches sid) nil)
           new-store (render-twig (twig-container db session records) old-store)
           changes (diff-twig old-store new-store {:key :id})]
       (when config/dev? (println "Push to" sid (count records)))
       (if (not= changes [])
         (do
          (wss-send! sid {:kind :patch, :data changes})
          (swap! *client-caches assoc sid new-store)))))))

(defn render-loop! []
  (when (not (identical? @*reader-reel @*reel))
    (reset! *reader-reel @*reel)
    (sync-clients! @*reader-reel))
  (delay! 0.2 render-loop!))

(defn run-server! []
  (wss-serve!
   (:port config/site)
   {:on-open (fn [sid socket]
      (dispatch! :session/connect nil sid)
      (js/console.info "New client.")),
    :on-data (fn [sid action]
      (case (:kind action)
        :op (dispatch! (:op action) (:data action) sid)
        (println "unknown data" action))),
    :on-close (fn [sid event]
      (js/console.warn "Client closed!")
      (dispatch! :session/disconnect nil sid)),
    :on-error (fn [error] (.error js/console error))}))

(defn watch-storage! []
  (let [filepath storage-file]
    (reset! *storage-md5 (md5 (fs/readFileSync filepath "utf8")))
    (gaze
     filepath
     (fn [error watcher]
       (if (some? error)
         (js/console.log error)
         (.on ^js watcher "changed" (fn [_] (on-file-change! filepath))))))))

(defn main! []
  (println "Running mode:" (if config/dev? "dev" "release"))
  (dotnet/config)
  (validate! (read-string (fs/readFileSync storage-file "utf8")))
  (if (= js/process.env.op "compile")
    (do
     (println (.yellow chalk "Compilation only mode!"))
     (locales/generate-files! (:db @*reel))
     (persist-db!))
    (do
     (run-server!)
     (render-loop!)
     (comment .on js/process "SIGINT" on-exit!)
     (comment js/setInterval #(persist-db!) (* 60 1000 10))
     (println
      "Server started. Open editor on"
      (.blue chalk "http://fe.jimu.io/locales-editor/"))
     (check-version!)
     (watch-storage!))))

(defn reload! []
  (println "Code updated.")
  (reset! *reel (refresh-reel @*reel initial-db updater))
  (sync-clients! @*reader-reel))
