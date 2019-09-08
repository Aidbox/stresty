
(ns matcho
  (:require [clojure.string :as s]))

(defn smart-explain-data [p x]
  (cond

    (and (string? x) (instance? java.util.regex.Pattern p))
    (when-not (re-find p x)
      {:expected (str "match regexp: " p) :but x})

    (fn? p)
    (when-not (p x)
      {:expected (pr-str p) :but x})

    :else (when-not (= p x)
            {:expected p :but x})))

(defn- match-recur [errors path x pattern]
  (cond
    (and (map? x)
         (map? pattern))
    (let []
      (reduce (fn [errors [k v]]
                (let [path (conj path k)
                      ev   (get x k)]
                  (match-recur errors path ev v)))
              errors pattern))

    (and (sequential? pattern)
         (sequential? x))
    (let []
      (reduce (fn [errors [k v]]
                (let [path (conj path k)
                      ev   (nth (vec x) k nil)]
                  (match-recur errors path ev v)))
              errors
              (map (fn [x i] [i x]) pattern (range))))

    :else (let [err (smart-explain-data pattern x)]
            (if err
              (conj errors (assoc err :path path))
              errors))))

(defn- match-recur-strict [errors path x pattern]
  (cond
    (and (map? x)
         (map? pattern))
    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev   (get x k)]
                (match-recur-strict errors path ev v)))
            errors pattern)

    (and (sequential? pattern)
         (sequential? x))
    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev   (nth (vec x) k nil)]
                (match-recur-strict errors path ev v)))
            (if (= (count pattern) (count x))
              errors
              (conj errors {:expected "Same number of elements in sequences"
                            :but      (str "Got " (count pattern)
                                           " in pattern and " (count x) " in x")
                            :path     path}))
            (map (fn [x i] [i x]) pattern (range)))

    :else (let [err (smart-explain-data pattern x)]
            (if err
              (conj errors (assoc err :path path))
              errors))))

(defn built-in-fn [fn-name]
  (if-let [func (ns-resolve 'clojure.core (symbol fn-name))]
    #( func %)
    (throw  (ex-info (str "Unknown function name '" fn-name "'") {:type :unknown-fn-name}))))

(defn replace-with-functions [template]
  (cond
    (and (string? template) (s/ends-with? template "?") (built-in-fn template)) (built-in-fn template)
    (and (string? template) (s/starts-with? template "#")) #(re-matches (java.util.regex.Pattern/compile (subs template 1)) %)
    (map? template) (reduce-kv #(assoc %1 %2 (replace-with-functions %3)) {} template)
    ;; list?
    :else template))

(comment
  (replace-with-functions {:fn number? :str "number?"})

  (replace-with-functions {:headerrs [] :status 200 :body {:name "str?"}})

  (reduce-kv #(assoc %1 %2 (+ 1 %3)) {} {:a 1 :b 2})

  (cond (and (str? template) (not (nil? (get funcs template)))) (get funcs template))

  (println "hello!!!")

  (#(re-matches (java.util.regex.Pattern/compile "aaa") %) "aaa")

  (def wow #(re-matches (java.util.regex.Pattern/compile "aaa") %))

  (wow "aaa")

  (java.util.regex.Pattern/compile "aaa")

  ((resolve "string?") "wow")


  (match {:a true} {:a number?})

  )



(defn match
  "Match against each pattern"
  [x & patterns]
  (reduce (fn [acc pattern] (match-recur acc [] x (replace-with-functions pattern))) [] patterns))



