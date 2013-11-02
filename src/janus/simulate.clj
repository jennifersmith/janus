(ns janus.simulate
  (:require [liberator.core :refer [resource]]
            [ring.util.serve :refer [serve* stop-server]]
            [compojure.core :refer [routes ANY]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]])
  (:import [java.io Closeable]))

;;HACK! Do someting better with this
(defn fixup-endpoint [endpoint]
  (println endpoint "Here")
  (last (re-matches #"(http://localhost:\d+)(.*)" endpoint)))

(defn create-resource-handler [endpoints resources]
  (let [routes (apply routes (map #(ANY (fixup-endpoint %1) [] %2) endpoints resources))]
    (-> routes
        (wrap-json-response))))

;; for when I can be bothered to log properly
(defn log [& message] (println (apply str "[SIMULATOR]:\t" message)))

(defn create-resource-for-contract [& contract]
  (resource 
            :available-media-types ["application/json"]
            :handle-ok {:cities
                        [{:name "Melbourne" :temp "22 ºC"}
                         {:name "London" :temp "15 ºC"}
                         {:name "Chicago" :temp "12 ºC"}]}))


(defn simulate [{:keys [name contracts]} port]
  (log "Starting simulator of service " name)
  (let [contract-resources (map create-resource-for-contract contracts)
        handler (create-resource-handler (map :endpoint contracts) contract-resources)
        server (serve* handler port true)]
    (reify Closeable
      (close [this]
        (stop-server)
        (log "Shutting down simulation of service " name)))))
