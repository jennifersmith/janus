(ns janus.generators
  (:require [clojure.data.generators :as generators]
            [json-path.parser]))

(defmulti generate-data first)

(defmethod generate-data :content-type [& _] {})

;; todo; sort this crap out. if you need an array say you need an array :D
(defn crappy-temp-object-gen [clauses]
  (take (generators/uniform 0 10)
        (repeatedly #(apply merge {} (map generate-data clauses)))))

(def type->generator {:string (fn [& args] (generators/string)) 
                      :number (fn [& args] (generators/int))
                      :object  crappy-temp-object-gen})

(defmethod generate-data :of-type [[_ data-type args]]
  ((type->generator data-type) args))

;; only dealing with really simple jsonpath here
(defn json-path-to-assoc-path [json-path]
  (let [[_ [_ & segments]] (json-path.parser/parse-path json-path)]
    (->> segments
         (partition 2)
         (map (comp last last))
         (map keyword))))

(defmethod generate-data :path [[_ path & matching-args]]
  (assoc-in {} (json-path-to-assoc-path path) (generate-data matching-args)))

(defmethod generate-data :body [[_ clauses]]
  (apply merge (map generate-data clauses)))
