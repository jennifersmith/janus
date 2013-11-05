(ns janus.dsl)

(defn matching-jsonpath 
  ([path match expected]
     [:json-path path match expected])
  ([path match expected & children]
     [:json-path path match expected (vec children)]))

(defn content-type [type] [:content-type type])
(defn equal-to [expected] [:equal-to expected])

(defn url [path]
  [:property {:name "url" :value path}])

(defn method [method]
  [:method method])

(defn status [status]
  [:status status])

(defn serialization [s11n]
  [:property {:name "serialization" :value s11n}])


;; not a lot going on now...
(defn body [& clauses]
  [:body clauses])

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
