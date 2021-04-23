(ns stresty.actions.core)

(defmulti run-action (fn [ztx state args] (or (:type args) 'sty/http)))

(defmethod run-action :default
  [ztx state args]
  {:error {:message (str "Action " (:type args) " is not implemented!")}})

;; (defn run-step [ztx env case step-key step]
;;   (let [state (or (get-in @ztx [:state (:zen/name env) (:zen/name case)]) {})
;;         step (stresty.sci/eval-data {:namespaces {'sty {'step step 'case case 'state state}}} step)
;;         req {:method (:method step)
;;              :url (str (:base-url env) (:uri step))
;;              :headers {"content-type" "application/json"}
;;              :body (when-let [b (:body step)]
;;                      (cheshire.core/generate-string b))}
;;         ev-base {:type 'sty/on-step-start :env env :case case
;;                  :step (assoc step :id step-key)
;;                  :verbose (get-in @ztx [:opts :verbose])
;;                  :request req}]
;;     (fmt/emit ztx ev-base)
;;     (try
;;       (let [resp (-> (http/request req)
;;                      (update :body (fn [x] (when x (cheshire.core/parse-string x keyword)))))]
;;         (swap! ztx
;;                (fn [old]
;;                  (update-in old [:state (:zen/name env) (:zen/name case)]
;;                             (fn [state] (assoc state step-key resp)))))

;;         (if-let [err (:error resp)]
;;           (fmt/emit ztx (assoc ev-base :type 'sty/on-step-exception :exception err))
;;           (if-let [matcho (:response step)]
;;             (let [errors (stresty.matcho/match ztx state resp matcho)]
;;               (if-not (empty? errors)
;;                 (fmt/emit ztx (assoc ev-base :type 'sty/on-step-fail :errors errors :response resp))
;;                 (fmt/emit ztx (assoc ev-base :type 'sty/on-step-success :response resp))))
;;             (fmt/emit ztx (assoc ev-base :type 'sty/on-step-response :response resp)))))
;;       (catch Exception e
;;         (fmt/emit ztx (assoc ev-base :type 'sty/on-step-exception :exception e))))))
