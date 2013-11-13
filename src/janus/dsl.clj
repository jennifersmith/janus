(ns janus.dsl)

(defn matching-jsonpath 
  ([path match expected]
     [:json-path path match expected])
  ([path match expected & children]
     [:json-path path match expected (vec children)]))

(defn fn-based-clause [fn context subclauses]
  [:fn fn context (vec subclauses)])

(defn predfn-based-clause [fn context]
  [:predfn fn context])

(def keyword->typecheck {:number number? :map map? :collection coll? :string string?})

(defn with-length-between [from to]
  [:with-length-between from to])

(defn of-type [type] 
  (if-let [checker  (keyword->typecheck type) ]
    (predfn-based-clause checker {:type type})
    (throw (new Exception (str "No matching type checking fn found for " type)))))

(defn with-range [min max]
  [:in-range {:min min :max max}])

(defn should-have  [fn & subclauses]
  (fn-based-clause fn {:key fn} subclauses))

(defn should-match [regex]
  [:all
   [(of-type :string)
    (predfn-based-clause #(re-matches regex %) {:matches regex})]])

(defn each [& subclauses] 
  [:each subclauses])

(defn equal-to [expected] [:equal-to expected])

(defn url [path]
  [:property {:name "url" :value path}])

(defn method [method]
  [:method method])

(defn status [status]
  [:status status])

(defn serialization [s11n]
  [:property {:name "serialization" :value s11n}])


(defn json-body [& clauses]
  [:json-body clauses])

(defn header [name value]
  [:header {:name name :value value}])

;; these actually dont do a hell of a lot
(def request  vector)
(def response vector)

(defn contract 
  ([name endpoint request response] (assoc (contract name request response) :endpoint endpoint)) 
  ([name request response] {:name name :request request :response response}))

(defn service [name & contracts]
  {:name name
   :contracts (vec contracts)})

(defn construct-domain [dsl-form]
  (let [dsl-ns (create-ns 'dsl-defn)
        compiled (binding [*ns* dsl-ns]
                   (eval '(clojure.core/refer 'clojure.core))
                   (eval '(refer 'janus.dsl))
                   (eval dsl-form))]
    (remove-ns 'dsl-defn)
    compiled))
