(ns janus.simulate
  (:require [liberator.core :refer [resource]]
            [ring.util.serve :refer [serve* stop-server]]
            [compojure.core :refer [routes ANY]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [janus.generators :refer [generate-data]])
  (:import [java.io Closeable]))

;; copied from verify... basically saying this needs to be a map not tuples!
;; also note that we do actually need to "
(defn multi-property [prop-name contract]
  (->> contract
       (filter #(= prop-name (first %)))
       (map last)))

;; ach this is a sideaff
(defn property-new [prop-name contract]
  (first (multi-property prop-name contract)))

;;HACK! Do someting better with this
(defn fixup-endpoint [endpoint]
  (last (re-matches #"(http://localhost:\d+)(.*)" endpoint)))

(defn create-resource-handler [endpoints resources]
  (let [routes (apply routes (map #(ANY (fixup-endpoint %1) [] %2) endpoints resources))]
    (-> routes
        (wrap-json-response))))

;; for when I can be bothered to log properly
(defn log [& message] (println (apply str "[SIMULATOR]:\t" message)))

(defmulti build-response first)

(defmethod build-response :json-body [expected]
  (fn [response]
    (assoc response
      :body (generate-data expected))))

(defmethod build-response :header [[_ {:keys [name value]}]]
  (fn [response]
    (assoc-in response [:headers name] value)))

(defmethod build-response :status [[_ value]]
  (fn [response]
    (assoc response :status value)))

(defn create-resource-for-contract [{expected-response-clauses :response expected-request :request}]
  (let [create-response (apply comp (map build-response expected-response-clauses))]
    (fn [request]
      ;; todo : validate request
      (create-response (response nil)))))


(defn add-client-origin-policy [client-origin handler]
  (if (nil? client-origin)
    handler
    (wrap-cors handler :access-control-allow-origin (re-pattern client-origin))))

(defn simulate [{:keys [name contracts]} & configs]
  (log "Starting simulator of service " name)
  (let [
        {:keys [port client-origin]} (apply hash-map configs)
        contract-resources (map create-resource-for-contract contracts)
        handler (create-resource-handler (map :endpoint contracts) contract-resources)
        server (serve* (add-client-origin-policy client-origin handler) port true)]
    (reify Closeable
      (close [this]
        (stop-server)
        (log "Shutting down simulation of service " name)))))
