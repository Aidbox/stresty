(ns stresty.actions.core
  (:require [clj-http.lite.client :as http]
            [stresty.format.core :as fmt]
            [zen.core :as zen]
            [stresty.sci]
            [cheshire.core])
  (:import java.net.ConnectException))

(defmulti run-action (fn [ztx ctx args] (or (:act args) 'sty/http)))

(defmethod run-action :default
  [ztx ctx args]
  {:error {:message (format "Action '%s is not implemented!" (:type args))}})

(defn action [ztx ctx args]
  (let [tp (:act args)]
    (if-let [schema (zen/get-symbol ztx (or (:act args) 'sty/http))]
      (let [{errors :errors :as res} (zen/validate ztx #{'sty/action tp} args)]
        (if (empty? errors)
          (run-action ztx (assoc ctx :schema schema) args)
          {:error {:message "Wrong action parameters" :errors errors}}))
      {:error {:message (format "Action '%s is not defined " tp)}})))


(defmethod run-action 'sty/print
  [ztx {env :env case :case state :state} args]
  (if-let [pth (:path args)]
    {:result {:path pth
              :value (get-in state pth)}}
    (if (:expression args)
      {:result args}
      {:result nil})))

(defmethod run-action 'sty/http
  [ztx {env :env case :case state :state} args]
  (let [url (str (:base-url env) (:url args))
        meth (:method args)
        {{user :user pass :password} :basic-auth} env]
    (try
      (let [req (cond->
                    {:method (if (= :patch meth) :post meth)
                     :url url
                     :throw-exceptions false
                     :headers (merge (->> (:headers args)
                                          (reduce (fn [acc [k v]]
                                                    (if (keyword? k)
                                                      (assoc acc (name k) (str v))
                                                      (assoc acc k (str v))))
                                                  (cond-> {"content-type" "application/json"}
                                                    (= :patch meth) (assoc "X-HTTP-Method-Override" "PATCH")))))}
                  (:body args)
                  (assoc :body (cheshire.core/generate-string (:body args)))
                  (and user pass) (assoc :basic-auth [user pass]))
            _ (fmt/emit ztx {:type 'sty.http/request :source args :method (:method args) :url url :basic-auth (and user pass true)})
            resp (-> (http/request req)
                     (update :body (fn [x] (when x (cheshire.core/parse-string x keyword)))))]
        {:result (dissoc resp :headers)}) ;; TBD: support headers option;; too noisy
      (catch java.net.ConnectException _
        {:error {:message (format "Connection to %s is refused" url)}}))))
