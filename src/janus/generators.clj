(ns janus.generators
  (:require [clojure.data.generators :as generators]
            [json-path.parser]
            [clojure.walk :refer [prewalk]]))



(def type->generator {:string (fn [& args] (generators/string)) 
                      :number generators/int
                      :any generators/anything
                              :collection (fn [& args] [args])})

(defmulti generate-data-from-fn-context :context-type)
(defmulti generate-data first)

(defmethod generate-data-from-fn-context [:key] [{:keys [context subclauses]}]
  (let [
        subclause-to-use (or (first subclauses) [:any])
        data (generate-data subclause-to-use)]
    (if (> (count subclauses) 1)
      (println "Warning, dont use more than one subclause from a key fn"))
    {(:key context) data}))

(defmethod generate-data-from-fn-context [:type] [{:keys [context]}]
  ((type->generator (:type context))))



(defmethod generate-data :any [& _] (generators/anything))
(defmethod generate-data :content-type [& _] {})


(defmethod generate-data :of-type [[_ data-type args]]
  ((type->generator data-type) args))

;; only dealing with really simple jsonpath here
(defn json-path-to-assoc-path [json-path]
  (let [[_ [_ & segments]] (json-path.parser/parse-path json-path)]
    (->> segments
         (partition 2)
         (map (comp last last))
         (map keyword))))

(defmethod generate-data :json-path [[_ path & matching-args]]
  (assoc-in {} (json-path-to-assoc-path path) (generate-data matching-args)))

(defmethod generate-data :json-body [[_ clauses]]
  (apply merge (map generate-data clauses)))

;; TODO  : this is a massive hack we are basically going to map the subset of fns we support 
;; to generators... fn is the wrong abstraction at the contract level  - neeed to be more high leve;; and translate to fns etc at verif-time, so contracts should be {:has-property :foo}

(defmethod generate-data :fn [[_ keyword context clauses]]
  (generate-data-from-fn-context 
   {:context-type (keys context)  
    :context context :subclauses clauses}))

;; code dupe alert
(defmethod generate-data :predfn [[_ keyword context clauses]]
  (generate-data-from-fn-context 
   {:context-type (keys context)  
    :context context :subclauses clauses}))

;; Assumption : all result in KVPs ... seems like main usecase so far
;; also lazy... so need to probably put in some limits somewhere
(defmethod generate-data :each [[_ clauses]]
  (take (generators/uniform 0 1000)
        (repeatedly
         #(reduce merge (map generate-data clauses)))))

;;;========

;; redoing the massive hack. Now we are translating tuples of constraints into maps. 
;; because we have to control the evaluation of each one - for e.g. length has to come after the each specificier - so push into a map and then pass that to generator
;; This is proving that a tree might be better model for a contract

;;!!


(defn literal-generator-spec [type]
  (fn [current]
    (merge
     current
     {:type :generator :generator (type->generator type)})))

(defmulti generate-from :type)

(defmulti create-generator-spec first)
(defmulti create-generator-spec-from-fn-context :context-type)

(defn generate-data [body-constraints]
  (let [generator-spec (create-generator-spec body-constraints)]
    (prewalk generate-from (generator-spec {}))))

(defmethod create-generator-spec :any [& _] 
  (literal-generator-spec :any))

(defmethod create-generator-spec :predfn [[_ keyword context clauses]]
  (create-generator-spec-from-fn-context 
   {:context-type (keys context)  
    :context context 
    :subclauses clauses}))
(defmethod create-generator-spec :fn [[_ keyword context clauses]]
  (create-generator-spec-from-fn-context 
   {:context-type (keys context)  
    :context context 
    :subclauses clauses}))

(defmethod create-generator-spec :json-body [[_ clauses]]
  (fn [current]
    ((apply comp (map create-generator-spec clauses)) current)))


(defmethod create-generator-spec-from-fn-context [:key] [{:keys [context subclauses]}]
  (let [value-clauses
        ((apply comp (map create-generator-spec  subclauses)) { :type :any})
        ]
    (fn [current]
      (let [with-type (merge current {:type :object})]
        (merge-with concat with-type {:properties [value-clauses]
                                      :property-keys [(:key context)]})))))

(defmethod create-generator-spec-from-fn-context [:type] [{:keys [context]}]
  (literal-generator-spec (:type context)))

(defmethod create-generator-spec :with-length-between [[_ min-length max-length]]
  (fn [current]
    (merge current {:type :array :min-length min-length :max-length max-length})))

(defmethod create-generator-spec :each [[_ clauses]]
  (let [each-spec ((apply comp (map create-generator-spec clauses)) {})]
    (fn [current]
      (merge current {:type :array :each-value each-spec}))))

(defmethod generate-from :default [x]  x)

(defmethod generate-from :generator [{:keys [generator]}]
  (generator))

(defmethod generate-from :object [{:keys [property-keys properties]}]
  (zipmap property-keys properties))

(defmethod generate-from :array [params ]
  (let [{:keys [each-value min-length max-length] } 
        (merge  {:max-length 1000 :min-length 0} params)]
    (take (- max-length min-length) (repeat each-value) )))
