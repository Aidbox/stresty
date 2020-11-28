(ns stresty.runner.clj-http.step
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [matcho]
            [b64]
            [clj-http.client :as http]))

;; (remove-ns 'stresty.runner.clj-http.step)

(defn parse-json-or-leave-string [s]
  (try
    (json/parse-string s keyword)
    (catch com.fasterxml.jackson.core.JsonParseException e
      s)))

(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})

(defn default-req-params [ctx]
  (let [config (:config ctx)]
    {:url (:url config)
     :redirect-strategy :none
     :throw-exceptions false
     :headers (merge {"content-type" "application/json"} (:headers config))}))


(defmulti auth (fn [ctx _ agent-name]
                 (get-in ctx [:config :agents agent-name :type])))

(defmethod auth 'stresty/basic-auth [ctx req-opts agent-name]
  (let [agnt (get-in ctx [:config :agents agent-name])]
    {:req-opts (update req-opts :headers
                      assoc "authorization" (str "Basic " (b64/encode (str (:client-id agnt) ":" (:client-secret agnt)))))}))


;; (defn get-user-data [ctx]
;;   (let [{body :body :as resp} (-> ctx
;;                                   auth-user-req
;;                                   http/request)
;;         json-resp             (json/parse-string body true)
;;         access-token          (:access_token json-resp)]
;;     (merge {:headers {"Authorization" (str "Bearer " access-token)}}
;;            (:userinfo json-resp))))


;; (defmethod auth 'stresty.aidbox/user-login [ctx req-opts agent-name]
;;   (let [agnt (get-in ctx [:config :agents agent-name])]
;;     {:req-opts (update req-opts :headers
;;                        assoc "authorization" (str "Basic " (b64/encode (str (:client-id agnt) ":" (:client-secret agnt)))))})


;;   {:url              (str (:base-url ctx) "/auth/token")
;;    :method           :post
;;    :throw-exceptions false
;;    :headers          {"content-type" "application/json"}
;;    :body             (json/generate-string {:username      (:user-id ctx)
;;                                             :password      (:user-secret ctx)
;;                                             :client_id     (:auth-client-id ctx)
;;                                             :client_secret (:auth-client-secret ctx)
;;                                             :grant_type    "password"})}
;;   (str (:base-url ctx) "/auth/token")

;;   )


(defmulti run-step (fn [ctx step] (:type step)))

(defn request [opts]
  (try
    (http/request opts)
    (catch Exception e
      (Throwable->map e))))

(defmethod run-step 'stresty/http-step [{config :config :as ctx} step]
  (prn "wow")
  (let [method (first (filter meths (keys step)))
        url (str (get-in ctx [:config :url]) (get step method))
        body (if (string? (:body step)) (:body step) (json/generate-string (:body step)))
        req-opts (cond-> (merge (default-req-params ctx)
                                {:url url
                                 :request-method (keyword (str/lower-case (name method)))})
                   body
                   (assoc :body body))
        agent-name (get step :agent :default)
        {req-opts :req-opts
         new-ctx :ctx}
        (if (get-in ctx [:config :agents agent-name]) (auth ctx req-opts agent-name) req-opts)
        ctx (or new-ctx ctx)
        _ (prn "...")
        resp (http/request req-opts)
        _ (prn "===")
        resp
        (cond-> {:status (:status resp)
                 :headers (reduce (fn [m [k v]] (assoc m (str/lower-case k) v)) {} (:headers resp))}
          (:body resp)
          (assoc :body (parse-json-or-leave-string (:body resp))))
        errs (if-let [m (:match step)] (matcho/match nil resp m) [])]
    {:resp resp
     :ctx ctx
     :errros errs}))


(defmethod run-step 'stresty.aidbox/sql-step [ctx step]
  (run-step ctx {:type 'stresty/http-step
                 :POST "/$sql"
                 :body (:sql step)
                 :match {:status 200}}))

(defmethod run-step 'stresty.aidbox/truncate-step [ctx step]
  (let [sql (str "TRUNCATE "
                 (str/join ", "
                           (map (fn [x] (str "\"" (if (keyword? x) (str/lower-case (name x)) x) "\""))
                                (:truncate step))))]
    (run-step ctx {:type 'stresty.aidbox/sql-step
                   :sql sql})))

(comment

  (run-step nil {:type 'stresty.aidbox/truncate-step :truncate [:Patient "Practitioner"]})


  (reduce (fn [m [k v]] (assoc m (str/lower-case k) v)) {} {"Wow" "q"})


  (def ccc
    (run-step
     {:config {:url "https://little.aidbox.app"
               :agents {:default {:type 'stresty/basic-auth
                                  :client-id "basic"
                                  :client-secret "secret"}}}}
     {:type 'stresty/http-step
      :GET "/Patient"
      :match {:status 200}}))


  (matcho/match nil ccc {:status 200})

  ccc


  (let [r {:body ["wow"]}]
    (cond-> {}
      (:body r)
      (assoc :body (if (string? ))(json/generate-string (:body r)))))



  (meta
   (try
     (http/request
      {:url "http://localhost:8080"
       :ignore-unknown-host? true
       :redirect-strategy :none
       :throw-exceptions false})
     (catch Exception e
       (Throwable->map e)
       )))


  42

(http/get "http://localhost:1234" {:ignore-unknown-host? true})


(http/get "https://little.aidbox.wow" {:ignore-unknown-host? true})



(http/get "http://example.invalid" {:ignore-unknown-host? true})
;; => nil
  )


