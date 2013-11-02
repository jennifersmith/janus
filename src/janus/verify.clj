(ns janus.verify
  [:require [janus.json-response]
   [json-path]
   [clojure.data.json :as json]
   [clojure.data.xml :as xml]
   [clj-http.client :as http]])

(defn extract-clause [clause contract context]
  (filter #(= clause (nth % 0))
          (concat (:clauses contract) (:clauses context))))

(defmulti check-clause (fn [response clause] (nth clause 0)))

(defmethod check-clause :status [response clause]
  (let [actual (:status response)
        expected (nth clause 1)]
    (if (not= expected actual)
      (str "Expected status " expected ". Got status " actual))))

(defmethod check-clause :header [response clause]
  (let [[_ header-name comparison expected] clause
        actual (-> response :headers (get header-name))]
    (cond
     (= :equal-to comparison) (if (not= expected actual)
                               (str "Expected header '" header-name "' to equal '" expected "'. Got '" actual "'.")))))

(defn property [prop-name contract context]
  (:value (first (filter #(= prop-name (:name %))
                         (concat (:properties contract) (:properties context))))))

(defn errors-in-envelope [response contract context]
  (concat
   (map (partial check-clause response)
        (extract-clause :status contract context))
   (map (partial check-clause response)
        (extract-clause :header contract context))))

(defn errors-in-body [response contract context]
  (let [doc-type (-> response :headers (get "content-type"))
        body (:body response)
        clauses (concat (:clauses contract) (:clauses context))]
    (cond
     (or (re-seq #"^application/json" doc-type)
         (re-seq #"\+json" doc-type)
         (= (property "serialization" contract context) :json)) (janus.json-response/verify-document body clauses)
     :else [(str "Unable to verify documents of type '" doc-type "'")])))

(defn headers-from [contract context]
  (reduce (fn [acc v] (conj acc {(:name v) (:value v)}))
          {}
          (concat (:headers contract) (:headers context))))

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

(defn build-request [request endpoint context]
  {:method (property "method" request context),
   :url endpoint
   :headers (headers-from request context)
   :body (body-from request context)
   :throw-exceptions false})

(defn validate-response [response actual-response context]
  (let [envelope-errors (errors-in-envelope actual-response response context)
        body-errors (errors-in-body actual-response response context)]
    (take 1 (concat envelope-errors body-errors))))

;; todo :endpoint a very separate part of contract  when have 'transitive' contracts
(defn verify-contract [{:keys [request response name endpoint]} context]
  (let [
        request (build-request request endpoint context)
        actual-response (http/request request)
        errors (validate-response response actual-response context)
]
    (if (empty? errors)
      [name :succeeded]
      [name :failed errors])))

;; todo: remove context - should mash stuff up on creation of the contract
(defn verify-service [service context]
  (let [errors (filter #(= :failed (nth % 1))
                       (map #(verify-contract % context) (:contracts service)))]
    (if (empty? errors)
      [(:name service) :succeeded]
      [(:name service) :failed errors])))

;; (fact
;;   (verify-service {:name "simple JSON service"
;;                    :contracts [{:name "GET document"
;;                                 :properties [{:name "method" :value :get}
;;                                              {:name "url" :value "http://localhost:4568/service"}]
;;                                 :headers [{:name "Content-Type" :value "application/json"}]
;;                                 :clauses [[:path "$..id" :of-type :number]
;;                                           [:path "$..features[*]" :matching #"[a-z]"]]}]} {}) => empty?)
