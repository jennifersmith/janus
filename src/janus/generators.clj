(ns janus.generators
  (:require [clojure.data.generators :as generators]
            [json-path.parser]))



(def type->generator {:string (fn [& args] (generators/string)) 
                      :number (fn [& args] (generators/int))
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
  (take (generators/uniform 0 65000)
        (repeatedly
         #(reduce merge (map generate-data clauses)))))


