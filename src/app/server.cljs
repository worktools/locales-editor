
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
            [fipp.edn :refer [pprint]]
            [clojure.string :as string]
            [favored-edn.core :refer [write-edn]]
            ["javascript-natural-sort" :as naturalSort]
            ["latest-version" :as latest-version]
            ["chalk" :as chalk]
            ["axios" :as axios]
            ["md5" :as md5]
            ["gaze" :as gaze]
            [clojure.set :refer [intersection difference]]
            [cumulo-util.core :refer [id! repeat! unix-time! delay!]]
            [ws-edn.server :refer [wss-serve! wss-send! wss-each!]]
            [cumulo-util.file :refer [write-mildly! get-backup-path! merge-local-edn!]]
            [recollect.diff :refer [diff-twig]]
            [recollect.twig :refer [render-twig]]
            [app.twig.container :refer [twig-container]])
  (:require-macros [clojure.core.strint :refer [<<]]))

(defonce *client-caches (atom {}))

(def storage-file (path/join js/process.env.PWD "locales.edn"))

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
        version (.-version pkg)]
    (-> (latest-version (.-name pkg))
        (.then
         (fn [npm-version]
           (if (= npm-version version)
             (println "Running latest version" version)
             (println
              (.yellow
               chalk
               (<<
                "New version ~{npm-version} available, current one is ~{version} . Please upgrade!\n\nyarn global add @jimengio/locales-editor\n")))))))))

(defn format-keys [xs] (if (empty? xs) "" (str "(" (string/join ", " xs) ")")))

(defn lines-sorter [a b]
  (set! (.-insensitive naturalSort) true)
  (if (= (string/lower-case a) (string/lower-case b)) (compare a b) (naturalSort a b)))

(defn get-local-file [locales lang]
  (let [locale-keys (keys locales)]
    (->> locale-keys
         (map
          (fn [k]
            (let [v (get-in locales [k lang])]
              (str "  " k ": " (if (string/includes? v "\"") (str "'" v "'") (pr-str v)) ","))))
         (sort lines-sorter)
         (string/join "\n"))))

(defn persist-db! []
  (let [file-content (write-edn
                      (-> (:db @*reel) (assoc :sessions {}) (dissoc :saved-locales)))
        now (unix-time!)
        storage-path storage-file
        backup-path (get-backup-path!)]
    (reset! *storage-md5 (md5 file-content))
    (write-mildly! storage-path file-content)
    (write-mildly! backup-path file-content)
    (println "Saved file in" storage-path)))

(defn write-file! [filepath content] (fs/writeFile filepath content (fn [] )))

(defn generate-files! []
  (let [base js/process.env.PWD
        en-file (.join path base "enUS.ts")
        zh-file (.join path base "zhCN.ts")
        interface-file (.join path base "interface.ts")
        db (:db @*reel)
        locales (:locales db)
        en-content (str
                    "import { ILang } from \"./interface\";\nexport const enUS: ILang = {\n"
                    (get-local-file locales "enUS")
                    "\n};\n")
        zh-content (str
                    "import { ILang } from \"./interface\";\nexport const zhCN: ILang = {\n"
                    (get-local-file locales "zhCN")
                    "\n};\n")
        interface-content (let [locale-keys (keys locales)]
                            (str
                             "export interface ILang {\n"
                             (->> locale-keys
                                  (map (fn [k] (str "  " k ": string;")))
                                  (sort lines-sorter)
                                  (string/join "\n"))
                             "\n}\n"))]
    (println "Found" (count locales) "entries." "Genrating files...")
    (write-file! interface-file interface-content)
    (write-file! en-file en-content)
    (write-file! zh-file zh-content)
    (persist-db!)))

(defn show-changes! [locales saved-locales]
  (let [new-keys (set (keys locales))
        old-keys (set (keys saved-locales))
        common-keys (intersection new-keys old-keys)
        changed-keys (->> common-keys
                          (filter (fn [k] (not= (get locales k) (get saved-locales k)))))
        added-keys (difference new-keys old-keys)
        removed-keys (difference old-keys new-keys)]
    (println
     (do
      format-keys
      (<<
       "Added ~(count added-keys) keys~(format-keys added-keys), removed ~(count removed-keys) keys~(format-keys removed-keys), modified ~(count changed-keys) keys~(format-keys changed-keys).")))))

(defn translate-sentense! [text cb]
  (let [q (js/encodeURI text)
        salt (rand-int 100)
        app-key (-> @*reel :db :settings :app-id)
        app-secret (-> @*reel :db :settings :app-secret)
        sign (string/upper-case (md5 (str app-key text salt app-secret)))
        url (<<
             "http://openapi.youdao.com/api?q=~{q}&from=EN&to=zh_CHS&appKey=~{app-key}&salt=~{salt}&sign=~{sign}")]
    (when (or (nil? app-key) (nil? app-secret))
      (println "app-key and app-secret are required to use translation!")
      (js/process.exit 1))
    (comment println "data" salt (str app-key text salt app-secret) app-key app-secret sign)
    (comment -> (.get axios url) (.then (fn [result] (.log js/console (.-data result)))))
    (comment println url)
    (-> axios
        (.get
         "http://openapi.youdao.com/api"
         (clj->js
          {:params {:q text,
                    :from "zh-CHS",
                    :to "EN",
                    :appKey app-key,
                    :salt salt,
                    :sign sign}}))
        (.then
         (fn [response]
           (let [data (js->clj (.-data response) :keywordize-keys true)]
             (comment println "data" data)
             (if (= "0" (:errorCode data))
               (cb (-> data :translation first))
               (js/console.warn "Request failed:" data))))))))

(defn dispatch! [op op-data sid]
  (let [op-id (id!), op-time (unix-time!), db (:db @*reel)]
    (when config/dev? (println "Dispatch!" (str op) (subs (pr-str op-data) 0 140)))
    (try
     (cond
       (= op :effect/persist) (persist-db!)
       (= op :effect/codegen)
         (do
          (show-changes! (:locales db) (:saved-locales db))
          (generate-files!)
          (dispatch! :locale/mark-saved nil sid))
       (= op :effect/translate)
         (translate-sentense!
          (last op-data)
          (fn [result]
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
       (when config/dev? (println "Changes for" sid ":" changes (count records)))
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
  (when (= js/process.env.op "compile") (generate-files!) (js/process.exit 0))
  (run-server!)
  (render-loop!)
  (comment .on js/process "SIGINT" on-exit!)
  (comment js/setInterval #(persist-db!) (* 60 1000 10))
  (println
   "Server started. Open editer on"
   (.blue chalk "http://repo.tiye.me/chenyong/locales-editor/"))
  (check-version!)
  (watch-storage!))

(defn reload! []
  (println "Code updated.")
  (reset! *reel (refresh-reel @*reel initial-db updater))
  (sync-clients! @*reader-reel))
