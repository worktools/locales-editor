
(ns app.locales
  (:require [clojure.string :as string]
            ["javascript-natural-sort" :as naturalSort]
            ["fs" :as fs]
            ["path" :as path]
            [clojure.set :refer [intersection difference]]
            ["md5" :as md5]
            ["axios" :as axios])
  (:require-macros [clojure.core.strint :refer [<<]]))

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

(defn write-file! [filepath content] (fs/writeFile filepath content (fn [] )))

(defn generate-files! [db]
  (let [base js/process.env.PWD
        en-file (.join path base "enUS.ts")
        zh-file (.join path base "zhCN.ts")
        interface-file (.join path base "interface.ts")
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
    (write-file! zh-file zh-content)))

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

(defn translate-sentense! [text settings cb]
  (let [q (js/encodeURI text)
        salt (rand-int 100)
        app-key (:app-id settings)
        app-secret (:app-secret settings)
        sign (string/upper-case (md5 (str app-key text salt app-secret)))
        url (<<
             "http://openapi.youdao.com/api?q=~{q}&from=EN&to=zh_CHS&appKey=~{app-key}&salt=~{salt}&sign=~{sign}")]
    (println "settings" settings)
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
