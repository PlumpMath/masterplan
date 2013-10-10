(ns masterplan.services
  (:require [masterplan.indexeddb :as idb]
            [io.pedestal.app.protocols :refer [put-message]]
            [io.pedestal.app.messages :as msg]))

;; The services namespace responsible for communicating with back-end
;; services. It receives messages from the application's behavior,
;; makes requests to services and sends responses back to the
;; behavior.
;;
;; This namespace will usually contain a function which can be
;; configured to receive effect events from the behavior in the file
;;
;; app/src/masterplan/start.cljs
;;
;; After creating a new application, set the effect handler function
;; to receive effects
;;
;; (app/consume-effect app services-fn)
;;
;; A very simple example of a services function which echoes all events
;; back to the behavior is shown below.

(def db nil)

(defn services-fn [message queue]
  (if-not db (idb/open-database! "masterplan" 1
                                 (fn [{:keys [db-opened]}]
                                   (when db-opened
                                     (def db db-opened)
                                     (services-fn message queue))))
          (when-let [key (:requested-key message)]
            (idb/-get db
                      key
                      (fn [{{:keys [parent children] :as mresult} :result}]
                        (when mresult
                                        ; clear
                          (put-message queue {msg/topic [:children] msg/type :set-value :value []})
                          (put-message queue {msg/topic [:parent] msg/type :set-value :value nil})
                          (when parent
                            (idb/-get db
                                      parent
                                      (fn [{:keys [result]}]
                                        (when result
                                          (put-message queue {msg/topic [:parent]
                                                              msg/type :set-value
                                                              :value result}))
                                        (put-message queue {msg/topic [:main]
                                                              msg/type :set-value
                                                              :value mresult}))))
                          (when children
                            (doseq [child children]
                              (idb/-get db
                                        child
                                        (fn [{:keys [result]}]
                                          (when result
                                            (put-message queue {msg/topic [:children]
                                                                msg/type :add-value
                                                                :value (assoc result :parent mresult)}))))))))))))




;; During development, it is helpful to implement services which
;; simulate communication with the real services. This implementation
;; can be placed in the file:
;;
;; app/src/masterplan/simulated/services.cljs
;;
