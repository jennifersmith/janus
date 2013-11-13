(ns janus.generators
  (:require [clojure.data.generators :as generators]
            [json-path.parser]
            [clojure.walk :refer [prewalk]]))


(defn create-sizer [min max]
  (if (some nil? [min max])
    generators/default-sizer
    #(generators/uniform min max)))

(defn generate-string [{:keys [min-length max-length]}]
  (let [sizer (create-sizer min-length max-length)]
    (generators/string generators/printable-ascii-char sizer)))

(defn generate-number [{:keys [range] :or {:range [Integer/MIN_VALUE Integer/MAX_VALUE]}}]
  (apply generators/uniform range))

(def type->generator {:string generate-string
                      :number generate-number
                      :any (fn [params] (generators/anything))})

;;=====

(defmulti literal-generator :params)

;;;========

;; redoing the massive hack. Now we are translating tuples of constraints into maps. 
;; because we have to control the evaluation of each one - for e.g. length has to come after the each specificier - so push into a map and then pass that to generator
;; This is proving that a tree might be better model for a contract

;;!!


(defn literal-generator-spec [type]
  (fn [current]
    (merge
     current
     {:type :generator :generator-type type})))

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
        ((apply comp (map create-generator-spec  subclauses)) { :type :generator :generator-type :any})
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

(defmethod create-generator-spec :in-range [[_ {:keys [min max]}]]
  (fn [current]
    (merge current {:type :generator :generator-type :number :range [min max]})))

(defmethod create-generator-spec :each [[_ clauses]]
  (let [each-spec ((apply comp (map create-generator-spec clauses)) {})]
    (fn [current]
      (merge current {:type :array :each-value each-spec}))))

(defmethod generate-from :default [x]  x)

(defmethod generate-from :generator [{:keys [generator-type] :as params}]
  ((type->generator generator-type) params))

(defmethod generate-from :object [{:keys [property-keys properties]}]
  (zipmap property-keys properties))

(defmethod generate-from :array [params ]
  (let [{:keys [each-value min-length max-length] } 
        (merge  {:max-length 1000 :min-length 0} params)]
    (take (- max-length min-length) (repeat each-value))))
