(ns janus.verify
  [:require [janus.json-response]
   [json-path]
   [clojure.data.json :as json]
   [clojure.data.xml :as xml]
   [clj-http.client :as http]
   [clojure.data.json :as json]])

(defn excerpt [data]
  (apply str (take 100 (str data))))

(def content-type->parser {:json json/read-json})

(defn safe-parse [content-type raw]
  (if-let [parser (content-type->parser content-type)]
    (try      
      {:result :success :parsed (parser raw)}
      (catch Exception e
        {:result :failure 
         :message 
         [(str "Cannot parse as " content-type 
               (excerpt raw)
                ", exception message: " (.getMessage e))]}))
    [(str "Unrecognised content type " content-type)])) ;;NASSTY!

;; Todo : we should make request a map ... needs to group header stuff under headers first
;; when it is a map, it basically *is* a ring request which is convienient
(defn multi-property [prop-name contract]
  (->> contract
       (filter #(= prop-name (first %)))
       (map last)))

;; ach this is a sideaff
(defn property-new [prop-name contract]
  (first (multi-property prop-name contract)))

(defn extract-clause [clause contract context]
  (filter #(= clause (nth % 0))
          (concat (:clauses contract) (:clauses context))))

;;; gonna be strict on content types for now
(defn get-legacy-verifier [doc-type]
  (case doc-type
   :json janus.json-response/verify-document
   (constantly [(str "Unable to verify documents of type '" doc-type "'")])))

(def check-clause nil)
(defmulti check-clause (fn [response clause]  (nth clause 0)))

(defmethod check-clause :status [response [_ expected]]
  (let [actual (:status response)]
    (if (not= expected actual)
      [(str "Expected status " expected ". Got status " actual)]
      [])))

;; todo : only can check header equality...

(defmethod check-clause :header [response [_ {:keys [name value]}]]
  (let [ actual (-> response :headers (get name))]
   (if (not= value actual)
     [(str "Expected header '" name "' to equal '" value "'. Got '" actual "'.")]
     [])))

;;=======
(def check-body-clause nil)
(defmulti check-body-clause (fn [_ clause]  (first clause)))

(defmethod check-body-clause :default [_ [clause & _]]
  [(str "CONTRACT ERROR: " clause " is not an understood body clause")])

(defmethod check-body-clause :json-path [& _]
  (println "WARNING: using legacy matchers")
  [])

(defmethod check-body-clause :all [body [_ clauses]]
  (mapcat #(check-body-clause body %) clauses))

(defmethod check-body-clause :with-length-between [actual [_ from to]]
  (if-not (<= from (count actual) to)
    [(str "Expected " (excerpt actual) " to have length between " from " and " to)]
    []))


(defmethod check-body-clause :each [values [_ clauses]]
  (flatten (for [value values clause clauses]
             (check-body-clause value clause))))
;; TODO: Move content type to the head 
(defmethod check-body-clause :content-type [& _])
(defmethod check-body-clause :fn [actual [_ pred context subclauses]]
  (if-let [current (pred actual)]
    (do
      (mapcat #(check-body-clause current %) subclauses))
    [(str "Expected " context " to be non-nil on " (excerpt actual))]))

(defmethod check-body-clause :predfn [actual [_ pred context]]
  (try      
    (if-not (pred actual)
      [(str "Expected " actual " to be " context)]
      []
      )
    (catch Exception e
      [(str "Error applying predicate to "
            (excerpt actual)
            ", exception message: " (.getMessage e))])))
;;=======
;; legacy split here... we actually used to do a whole bunch in the json response stuff
;; I don't think need to as data is clj data structure - going to retire some of this stuf
;; can then tighten up a lot of verif

(defmethod check-clause :json-body [actual-response [_ body-clauses]]
  (let [ body (:body actual-response)
        legacy-verifier (get-legacy-verifier :json)
        {:keys [parsed result message]} (safe-parse :json body)]
    (if (= :success result)
      (concat
       (mapcat (partial check-body-clause parsed) body-clauses)
       (legacy-verifier body body-clauses))
      message)))

(defn property [prop-name contract context]
  (:value (first (filter #(= prop-name (:name %))
                         (concat (:properties contract) (:properties context))))))
;; see note above





(defn vec-to-el [[tag & attrs-and-els]]
  (let [attrs (if (map? (first attrs-and-els))
                (first attrs-and-els)
                {})
        els (if (map? (first attrs-and-els))
              (rest attrs-and-els)
              attrs-and-els)]
    (apply xml/element (concat [tag attrs]
                               (map (fn [el] (if (string? el)
                                               el
                                               (vec-to-el el)))
                                    els)))))

(defn to-xml [tree]
  (xml/emit-str (vec-to-el tree)))

;; not tested
(defn body-from [contract context]
  (let [body-def (if (contains? contract :body)
                   (:json-body contract)
                   (:json-body context))]
    (json/json-str (:data body-def))))

(defn build-headers [headers]
  (zipmap (map :name headers) (map :value headers)))
;; TODO : Not under test ;D
(defn build-request [request endpoint context]
  {:method (property-new :method request),
   :url endpoint
   :headers (build-headers (multi-property :header request))
   :body (body-from request context)
   :throw-exceptions false})

(def validate-response nil)

(defmulti validate-response (fn [_ response _] (:result response)))

(defmethod validate-response :ok [expected-response {:keys  [response]} context]
  (mapcat #(check-clause response %) expected-response))

(defmethod validate-response :socket-error [_ {:keys [error] } context]
  [error])

;; todo: lose stack trace ... bad?
(defn safe-request [request] 
  (try
    {:result :ok
     :response (http/request request)}
    (catch java.io.IOException e
      {:result :socket-error :error {:class (class e) :message (.getMessage e)}} )))

;; todo :endpoint a very separate part of contract  when have 'transitive' contracts
(defn verify-contract [{:keys [request response name endpoint]} context]
  (let [
        request (build-request request endpoint context)
        actual-response (safe-request request)
        errors (validate-response response actual-response context)]
    
    (if (empty? errors)
      {:result :succeeded}
      {:result :failed :errors errors})))

;; todo: remove context - should mash stuff up on creation of the contract

(defn verify-service [{:keys [name contracts]} context]
  (let [results (map #(verify-contract % context) contracts)]
    {:service name
     :results (zipmap (map :name contracts) results)}))

;; (fact
;;   (verify-service {:name "simple JSON service"
;;                    :contracts [{:name "GET document"
;;                                 :properties [{:name "method" :value :get}
;;                                              {:name "url" :value "http://localhost:4568/service"}]
;;                                 :headers [{:name "Content-Type" :value "application/json"}]
;;                                 :clauses [[:json-path "$..id" :of-type :number]
;;                                           [:json-path "$..features[*]" :matching #"[a-z]"]]}]} {}) => empty?)
