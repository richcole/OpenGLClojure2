(ns game.task-queue)

(def task-queue (atom []))

(defn post-action [task]
  (let [result (promise)
        action {:task task :result result}
        _ (dosync (swap! task-queue conj action))
        result-value (deref result)]
    (if (instance? RuntimeException result-value)
      (throw (RuntimeException. result-value))
      result-value)))

(defn read-action []
  (dosync
    (when (not (empty? (deref task-queue)))
      (let [action (first (deref task-queue))]
        (swap! task-queue rest)
        action))))

(defn read-execute-action []
  (let [action (read-action)]
    (when action
      (let [result (:result action)
            task (:task action)]
        (try
          (deliver result (task))
          (catch RuntimeException e (deliver result e)))))))

(defmacro gl-run [& cmd] `(post-action (fn [] ~@cmd)))