(ns janus.verify
  [:require [janus.json-response]
   [json-path]
   [clojure.data.json :as json]
   [clojure.data.xml :as xml]
   [clj-http.client :as http]])

(defn extract-clause [clause contract context]
  (filter #(= clause (nth % 0))
          (concat (:clauses contract) (:clauses context))))

(def check-clause nil)
(defmulti check-clause (fn [response clause] (println "HEEY" (nth clause 0)) (nth clause 0)))

(defmethod check-clause :status [response [_ expected]]
  (let [actual (:status response)]
    (if (not= expected actual)
      (str "Expected status " expected ". Got status " actual))))

;; todo : only can check header equality...

(defmethod check-clause :header [response [_ {:keys [name value]}]]
  (let [ actual (-> response :headers (get name))]
   (if (not= value actual)
     (str "Expected header '" name "' to equal '" value "'. Got '" actual "'.")
     [])))

(defn property [prop-name contract context]
  (:value (first (filter #(= prop-name (:name %))
                         (concat (:properties contract) (:properties context))))))

;; TODO : we should make request a map ... needs to group header stuff under headers first
;; when it is a map, it basically *is* a ring request which is convienient
(defn multi-property [prop-name contract]
  (->> contract
       (filter #(= prop-name (first %)))
       (map last)))
;; see note above
(defn property-new [prop-name contract]
  (first (multi-property prop-name contract)))

(defn errors-in-envelope [actual-response expected-response context]
  (map #(check-clause actual-response %) expected-response))

;;; gonna be strict on content types for now
(defn get-verifier [doc-type]
  (case doc-type
   :json janus.json-response/verify-document
   :else (constantly [(str "Unable to verify documents of type '" doc-type "'")])))

(defn errors-in-body [actual-response expected-response context]
  (let [ body (:body actual-response)
        body-clauses (property-new :body expected-response)
        verifier (get-verifier (property-new :of-type body-clauses))]

    (verifier body body-clauses)))


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

(defn body-from [contract context]
  (let [body-def (if (contains? contract :body)
                   (:body contract)
                   (:body context))]
    (cond
     (= (:type body-def) :string) (str (:data body-def))
     (= (:type body-def) :json) (json/json-str (:data body-def))
     (= (:type body-def) :xml) (to-xml (:data body-def)))))

(defn build-headers [headers]
  (zipmap (map :name headers) (map :value headers)))
;; TODO : Not under test ;D
(defn build-request [request endpoint context]
  {:method (property-new :method request),
   :url endpoint
   :headers (build-headers (multi-property :header request))
   :body (body-from request context)
   :throw-exceptions false})

(defn validate-response [response actual-response context]
  (let [envelope-errors (errors-in-envelope actual-response response context)
        body-errors (errors-in-body actual-response response context)]
    (concat envelope-errors body-errors)))

;; todo :endpoint a very separate part of contract  when have 'transitive' contracts
(defn verify-contract [{:keys [request response name endpoint]} context]
  (let [
        request (build-request request endpoint context)
        actual-response (http/request request)
        errors (validate-response response actual-response context)
]
    
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
;;                                 :clauses [[:path "$..id" :of-type :number]
;;                                           [:path "$..features[*]" :matching #"[a-z]"]]}]} {}) => empty?)
