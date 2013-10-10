(ns ^:shared masterplan.behavior
    (:require [clojure.string :as string]
              [io.pedestal.app :as app]
              [io.pedestal.app.util.platform :as p]
              [io.pedestal.app.dataflow :as d]
              [io.pedestal.app.messages :as msg]))

;; While creating new behavior, write tests to confirm that it is
;; correct. For examples of various kinds of tests, see
;; test/masterplan/behavior-test.clj.

;; Transforms

(defn set-selected [old-value message]
  (:select message))

(defn set-value [old-value message]
  (:value message))

(defn add-value [old-value message]
  (conj old-value (:value message)))

;; Effects

(defn request-values-from-db [selected]
  [{msg/topic [:indexeddb] :requested-key selected}])

;; Emitters

(defn init-main [_]
  [{:plan
    {:selected {}
     :form
     {:select
      {:transforms
       {:select [{msg/topic [:selected] (msg/param :select) {}}]}}}}}])

(def masterplan-app
  ;; There are currently 2 versions (formats) for dataflow
  ;; description: the original version (version 1) and the current
  ;; version (version 2). If the version is not specified, the
  ;; description will be assumed to be version 1 and an attempt
  ;; will be made to convert it to version 2.
  {:version 2
   :transform [[:select [:selected] set-selected]
               [:set-value [:main] set-value]
               [:set-value [:parent] set-value]
               [:set-value [:children] set-value]
               [:add-value [:children] add-value]]
   :effect #{[#{[:selected]} request-values-from-db :single-val]}
   :emit [{:init init-main}
          [#{[:parent]
             [:main]
             [:children]
             [:selected]}
           (fn [inputs]
             (concat ((app/default-emitter [:plan]) inputs)
                     [[:transform-disable
                       [:plan :form :select]
                       :select
                       [{msg/topic [:selected] (msg/param :select) ""}]]
                      [:transform-enable
                       [:plan :form :select]
                       :select
                       [{msg/topic [:selected] (msg/param :select) ""}]]]))]]})
