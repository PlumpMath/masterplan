(ns masterplan.core
  (:require [dommy.core :as dom]
            [goog.net.XhrIo :as xhr]
            [cljs.reader :refer [read-string]]
            [masterplan.communicator :refer [get-edn post-edn]]
            [cljs.core.async :as async :refer [chan close! put!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [dommy.macros :refer [sel sel1 node deftemplate]]))


; fire up a repl for the browser and eval namespace on top once connected
#_(do (ns masterplan.clojure.start)
      (require 'cljs.repl.browser)
      (cemerick.piggieback/cljs-repl
       :repl-env (doto (cljs.repl.browser/repl-env :port 9011)
                   cljs.repl/-setup)))

(defn hello
  [state]
  (js/alert "Hello from cljs!"))

#_(hello)

(defn- duration [{:keys [in out]}]
  (- (.getTime (:ts out)) (.getTime (:ts in))))

(defn task-timeline [parent task]
  (let [{:keys [id in out bgcolor]} task
        pdur (duration parent)
        rel-offset (* (/ (- (.getTime (:ts in))
                            (.getTime (:ts (:in parent))))
                         pdur)
                      100)
        rel-width (* (/ (duration task) pdur) 100)]
    [:div {:id id :style (dom/style-str {:position "relative"
                                         :top "30px"
                                         :left (str rel-offset "%")
                                         :width (str rel-width "%")
                                         :height "30px"
                                         :background-color bgcolor
                                         :border})}
;    [:span (:desc in)] id [:span (:desc out)]
     ]))

(defn- children [state task]
  (vals (select-keys state (:timeline task))))

(defn task-dom [state parent task]
  (conj (task-timeline parent task)
        (map #(task-dom state task %) (children state task))))

#_(task-dom @test-state
            (get @test-state "masterplan")
            (get @test-state "masterplan"))

(defn add-actions! [state task]
  (let [node (sel1 (str "#" (:id task)))
        tooltip (gensym "tooltip_")]
    (dom/listen! node
                 :mouseover
                 (fn [e]
                   (dom/append! js/document.body
                                [:div {:id tooltip
                                       :style
                                       (dom/style-str
                                        {:position "fixed"
                                         :background-color "grey"
                                         :z-index "10"
                                         :left (str (.-clientX e) "px")
                                         :top (str (.-clientY e) "px")
                                         })} (:id task)]))
                :mouseout (fn [e] (dom/remove! (sel1 (str "#" tooltip)))))
    (doseq [c (children state task)]
      (add-actions! state c))))

#_(dom/listen! (sel1 :#masterplan) :click (fn [e] (dom/set-text! (sel1 :#masterplan) "TEST")))
#_(dom/remove! (sel1 :#masterplan))
#_(init-view! @test-state)

(defn init-view! [state]
  (let [main (state (:main state))]
    (dom/append! (sel1 :#plan) (task-dom state main main))
    (add-actions! state main)))


(defn demo-state
  [] {:main "masterplan"
      "masterplan" {:id "masterplan"
                    :in {:ts (js/Date. 2013 10 1)
                         :desc "Startbedingungen sind ..."}
                    :out {:ts (js/Date. 2013 10 31)
                          :desc "Endbedingungen sind ..."}
                    :bgcolor "red"
                    :visible true
                    :timeline ["tagesplan"]}
      "tagesplan" {:id "tagesplan"
                   :in {:ts (js/Date. 2013 10 5)
                        :desc "Morgens"}
                   :out {:ts (js/Date. 2013 10 6)
                         :desc "Abends"}
                   :bgcolor "blue"
                   :timeline []}})

(comment
  (def test-state (atom (demo-state)))

  (init-view! @test-state))
