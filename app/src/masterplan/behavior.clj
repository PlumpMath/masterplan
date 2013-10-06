(ns ^:shared masterplan.behavior
    (:require [clojure.string :as string]
              [io.pedestal.app :as app]
              [io.pedestal.app.util.platform :as p]
              [io.pedestal.app.dataflow :as d]
              [io.pedestal.app.messages :as msg]))
;; While creating new behavior, write tests to confirm that it is
;; correct. For examples of various kinds of tests, see
;; test/masterplan/behavior-test.clj.

(def db {"Nachhaltiges Mannheim 2014"
         {:id "Nachhaltiges Mannheim 2014"
          :start (js/Date. 2013 01 01)
          :end (js/Date. 2014 01 01)
          :children ["Monatsplan"]}
         "Monatsplan"
         {:id "Monatsplan"
          :start (js/Date. 2013 10 1)
          :end (js/Date. 2013 11 1)
          :parent "Nachhaltiges Mannheim 2014"
          :children ["Kellnerschicht" "Kloputzen"]}
         "Kellnerschicht"
         {:id "Kellnerschicht"
          :start (js/Date. 2013 10 10)
          :end (js/Date. 2013 10 11)
          :parent "Monatsplan"}
         "Kloputzen"
         {:id "Kloputzen"
          :start (js/Date. 2013 10 20)
          :end (js/Date. 2013 10 21)
          :parent "Monatsplan"}
         })

(defn init-main [_]
  [{:plan
    {:selected {}
     :form
     {:select
      {:transforms
       {:select [{msg/topic [:selected] (msg/param :select) {}}]}}}}}])


(defn set-selected [old-value message]
  (:select message))

(defn new-main [_ inputs]
  (db (:new (d/old-and-new inputs [:selected]))))

(defn new-parent [_ inputs]
  (db (:parent (new-main nil inputs))))

(defn new-children [_ inputs]
  (let [main (new-main nil inputs)]
    (map #(assoc % :parent main)
         (map db (:children main)))))

(def masterplan-app
  ;; There are currently 2 versions (formats) for dataflow
  ;; description: the original version (version 1) and the current
  ;; version (version 2). If the version is not specified, the
  ;; description will be assumed to be version 1 and an attempt
  ;; will be made to convert it to version 2.
  {:version 2
   :transform [[:select [:selected] set-selected]]
   :derive #{[#{[:selected]} [:parent] new-parent]
             [#{[:selected]} [:main] new-main]
             [#{[:selected]} [:children] new-children]}
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

;; Once this behavior works, run the Data UI and record
;; rendering data which can be used while working on a custom
;; renderer. Rendering involves making a template:
;;
;; app/templates/masterplan.html
;;
;; slicing the template into pieces you can use:
;;
;; app/src/masterplan/html_templates.cljs
;;
;; and then writing the rendering code:
;;
;; app/src/masterplan/rendering.cljs

(comment
  ;; The examples below show the signature of each type of function
  ;; that is used to build a behavior dataflow.

  ;; transform

  (defn example-transform [old-state message]
    ;; returns new state
    )

  ;; derive

  (defn example-derive [old-state inputs]
    ;; returns new state
    )

  ;; emit

  (defn example-emit [inputs]
    ;; returns rendering deltas
    )

  ;; effect

  (defn example-effect [inputs]
    ;; returns a vector of messages which effect the outside world
    )

  ;; continue

  (defn example-continue [inputs]
    ;; returns a vector of messages which will be processed as part of
    ;; the same dataflow transaction
    )

  ;; dataflow description reference

  {:transform [[:op [:path] example-transform]]
   :derive    #{[#{[:in]} [:path] example-derive]}
   :effect    #{[#{[:in]} example-effect]}
   :continue  #{[#{[:in]} example-continue]}
   :emit      [[#{[:in]} example-emit]]}
  )
