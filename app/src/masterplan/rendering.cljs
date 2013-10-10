(ns masterplan.rendering
  (:require [domina :as dom]
            [domina.events :as domev]
            [io.pedestal.app.render.events :as events]
            [io.pedestal.app.render.push :as render]
            [io.pedestal.app.render.push.templates :as templates]
            [io.pedestal.app.render.push.handlers.automatic :as d]
            [io.pedestal.app.messages :as msg])
  (:require-macros [masterplan.html-templates :as html-templates]))

;; Load templates.

(def templates (html-templates/masterplan-templates))

(defn render-parent [renderer [_ path] transmitter]
  (let [parent (render/get-parent-id renderer (rest path)) ; TODO fix rest
        parent-id (render/new-id! renderer path)
        parent-html (templates/add-template renderer path (:parent templates))]
    (dom/append! (dom/by-id parent) (parent-html {:id parent-id :message ""}))))

(defn render-main [renderer [_ path] transmitter]
  (let [;; The renderer that we are using here helps us map changes to
        ;; the UI tree to the DOM. It keeps a mapping of paths to DOM
        ;; ids. The `get-parent-id` function will return the DOM id of
        ;; the parent of the node at path. If the path is [:a :b :c]
        ;; then this will find the id associated with [:a :b]. The
        ;; root node [] is configured when we created the renderer.
        parent (render/get-parent-id renderer (rest path))
        ;; Use the `new-id!` function to associate a new id to the
        ;; given path. With two arguments, this function will generate
        ;; a random unique id. With three arguments, the given id will
        ;; be associated with the given path.
        main-id (render/new-id! renderer path)
        ;; Get the dynamic template named :masterplan-page
        ;; from the templates map. The `add-template` function will
        ;; associate this template with the node at
        ;; path. `add-template` returns a function that can be called
        ;; to generate the initial HTML.
        main-html (templates/add-template renderer path (:main templates))]
    ;; Call the `html` function, passing the initial values for the
    ;; template. This returns an HTML string which is then added to
    ;; the DOM using Domina.
    (dom/append! (dom/by-id parent) (main-html {:id main-id :message ""}))))

(defn render-message [renderer [_ path _ new-value] transmitter]
  ;; This function responds to a :value event. It uses the
  ;; `update-t` function to update the template at `path` with the new
  ;; values in the passed map.
  (templates/update-t renderer path {:message (:id new-value)
                                     :start (when new-value (.toLocaleString (:start new-value)))
                                     :end (when new-value (.toLocaleString (:end new-value)))}))

(defn render-children [renderer [_ path _ new-value] transmitter]
  (let [child-html (templates/add-template renderer path (:child templates))
        parent-node (render/get-parent-id renderer (rest path))]
    (dom/destroy! (dom/by-class :child)) ; destroy all previous children
    (doseq [child new-value]
      (let [id (:id child)
            node (dom/nodes (child-html {:id id :message id}))
            parent-value (:parent child)
            parent-timespan (- (:end parent-value) (:start parent-value))
            timespan (- (:end child) (:start child))
            offset (- (:start child) (:start parent-value))]
        (dom/append! (dom/by-id parent-node) node)
        (dom/set-styles! node {:margin-left (str (* 100. (/ offset parent-timespan)) "%")
                               :width (str (* 100. (/ timespan parent-timespan)) "%")})))))

(defn add-handler [renderer [_ path transform-name messages] input-queue]
  (doseq [tl (dom/nodes (dom/by-class "timeline"))]
    (events/send-on :click
                    tl
                    input-queue
                    (fn []
                      ; extract id from inner content for now TODO
                      (msg/fill transform-name
                                messages
                                {:select (.-innerHTML (second (dom/children tl)))})))))

;; The data structure below is used to map rendering data to functions
;; which handle rendering for that specific change. This function is
;; referenced in config/config.edn and must be a function in order to
;; be used from the tool's "render" view.

(defn render-config []
  [;; All :node-create deltas for the node at :greeting will
   ;; be rendered by the `render-page` function. The node name
   ;; :greeting is a default name that is used when we don't
   ;; provide our own derives and emits. To name your own nodes,
   ;; create a custom derive or emit in the application's behavior.
   [:node-create [:plan :parent] render-parent]
   [:node-create [:plan :main] render-main]
   ;; All :node-destroy deltas for this path will be handled by the
   ;; library function `d/default-exit`.
   [:node-destroy   [:plan :parent] d/default-exit]
   [:node-destroy   [:plan :main] d/default-exit]
   [:node-destroy   [:plan :children] d/default-exit]
   ;; All :value deltas for this path will be handled by the
   ;; function `render-message`.
   [:value [:plan :parent] render-message]
   [:value [:plan :main] render-message]
   [:value [:plan :children] render-children]

   [:transform-enable [:plan :form :select] add-handler]
   [:transform-disable [:plan :form :select] add-handler]])

;; In render-config, paths can use wildcard keywords :* and :**. :*
;; means exactly one segment with any value. :** means 0 or more
;; elements.
