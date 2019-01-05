(ns opengl-clojure.test-queue-test
  (:require [clojure.test :refer :all]
            [opengl-clojure.task-queue :refer :all]))

(deftest post-and-retrieve-action
  (testing "Post and retrieve action"
    (let [result (future (post-action #(identity 1)))]
      (Thread/sleep 100)
      (read-execute-action)
      (is (= 1 (deref result)))))

(deftest post-action-and-retrieve-exception
  (testing "Post and retrieve action"
    (let [result (future (post-action #(throw (RuntimeException. "No milk left"))))]
      (Thread/sleep 100)
      (read-execute-action)
      (try
        (deref result)
        (is false "Expected exception thrown")
        (catch Throwable e
          (is (= "No milk left" (-> e .getCause .getMessage)))))))))
