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

(deftemplate task-timeline [task]
  (let [{:keys [id in out]} task]
    [:div {:id id} [:span (:desc in)] id [:span (:desc out)]]))


(defn init-view! [state]
  (doseq [t (vals state)]
    (dom/append! (sel1 :#plan) (task-timeline t))))


(defn demo-state
  [] {:main "masterplan"
      "masterplan" {:id "masterplan"
                    :in {:ts (js/Date.)
                         :desc "Startbedingungen sind ..."}
                    :out {:ts (js/Date.)
                          :desc "Endbedingungen sind ..."}
                    :timeline ["tagesplan"]}
      "tagesplan" {:id "tagesplan"
                   :in {}
                   :out {}
                   :timeline []}})

(comment
  (def test-state (atom (demo-state)))

  (init-view! @test-state))
