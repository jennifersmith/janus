(ns janus.simulate
  (:require [liberator.core :refer [resource]]
            [ring.util.serve :refer [serve* stop-server]]
            [compojure.core :refer [routes ANY]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response]]
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

(defmulti build-request first)
(defmethod build-request :body [expected]
  (fn [req]
    (assoc req
      :body (generate-data expected))))

(defmethod build-request :header [[_ {:keys [name value]}]]
  (fn [req]
    (assoc-in req [:headers name] value)))

(defn create-resource-for-contract [{expected-response :response expected-request :request}]
  (fn [actual-request]
    (apply comp (map build-request expected-response))))


(defn simulate [{:keys [name contracts]} port]
  (log "Starting simulator of service " name)
  (let [contract-resources (map create-resource-for-contract contracts)
        handler (create-resource-handler (map :endpoint contracts) contract-resources)
        server (serve* handler port true)]
    (reify Closeable
      (close [this]
        (stop-server)
        (log "Shutting down simulation of service " name)))))
