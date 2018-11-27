
(ns app.schema )

(def router {:name nil, :title nil, :data {}, :router nil})

(def translation {:key nil, :text nil})

(def session
  {:user-id nil,
   :id nil,
   :nickname nil,
   :query nil,
   :router (do router {:name :home, :data nil, :router nil}),
   :messages {},
   :translation (do translation nil)})

(def user {:name nil, :id nil, :nickname nil, :avatar nil, :password nil})

(def database
  {:sessions (do session {}),
   :users (do user {}),
   :locales {},
   :saved-locales {},
   :schema {:version "0.2.x"},
   :settings {:app-key nil, :app-secret nil}})
