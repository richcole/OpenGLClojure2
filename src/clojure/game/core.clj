(ns game.core
  (:require [game.task-queue :refer :all]
            [clojure.java.io :as io])
  (:import (org.lwjgl.opengl GL GL11 GL20 GL30 GL15)
           (org.lwjgl.glfw GLFWErrorCallback GLFW Callbacks GLFWWindowSizeCallbackI GLFWKeyCallbackI)
           (org.lwjgl.system MemoryStack MemoryUtil)
           (java.nio IntBuffer FloatBuffer)
           ))

(def global-window (ref nil))


(defprotocol Entity
  (render-entity [self window])
  (update-entity [self window ^Double delta-time]))

(def entity-list (ref []))

(defn entity-list-add [entity]
  (dosync (ref-set entity-list (conj (deref entity-list) entity))))

(defn entity-list-delete [entity-predicate]
  (dosync (ref-set entity-list (filter #(not (entity-predicate %)) (deref entity-list)))))

(defn entity-list-clear []
  (dosync (ref-set entity-list [])))

(defn entity-list-set [entity]
  (dosync (ref-set entity-list [entity])))

(defn entity-list-render [window]
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (doall (map #(render-entity % window) (deref entity-list)))
  nil)

(defn entity-list-update [window delta-time]
  (doall (map #(update-entity % window delta-time) (deref entity-list)))
  nil)

(deftype BasicTriangle []
  Entity
  (render-entity [this window]
    (GL11/glMatrixMode GL11/GL_PROJECTION)
    (GL11/glLoadIdentity)
    (GL11/glMatrixMode GL11/GL_MODELVIEW)
    (GL11/glLoadIdentity)
    (GL11/glBegin GL11/GL_TRIANGLES)
    (do
      (GL11/glColor3f 1 0 0)
      (GL11/glVertex3f -1 -1 0)

      (GL11/glColor3f 0 1 0)
      (GL11/glVertex3f 1 1 0)

      (GL11/glColor3f 0 0 1)
      (GL11/glVertex3f -1 1 0))
    (GL11/glEnd))
  (update-entity [this window delta-time]))

(defn main-loop [window delta-time]
  (dosync (ref-set global-window window))
  (read-execute-action)
  (entity-list-update window delta-time)
  (entity-list-render window)
  (GLFW/glfwSwapBuffers window)
  (GLFW/glfwPollEvents)
  )

(defn get-window-size [window]
  (let [^MemoryStack stack (MemoryStack/stackPush)]
    (try
      (let [^IntBuffer pWidth (.mallocInt stack 1)
            ^IntBuffer pHeight (.mallocInt stack 1)]
        (GLFW/glfwGetWindowSize window pWidth pHeight)
        [(.get pWidth 0) (.get pHeight 0)])
      (finally (.close stack)))))

(defn on-keyboard-input [window ^Integer key ^Integer scan-code ^Integer action ^Integer mods]
  (comment (condp = action
    GLFW/GLFW_PRESS (println "Press " (char key) action)
    GLFW/GLFW_RELEASE (println "Release " (char key) action)
    nil)
  ))

(defn run []
  (future
    (.set (GLFWErrorCallback/createPrint System/err))
    (GLFW/glfwInit)
    (GLFW/glfwDefaultWindowHints)
    (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
    (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)
    (let [window (GLFW/glfwCreateWindow 300 300 "Hello World!" 0 0)]
      (when window
        (try
          (GLFW/glfwMakeContextCurrent window)
          (GLFW/glfwSwapInterval 1)
          (GLFW/glfwShowWindow window)
          (GL/createCapabilities)
          (GL11/glClearColor 0.0 0.0 0.0 0.0)
          (GLFW/glfwSetWindowSizeCallback
            window
            (reify GLFWWindowSizeCallbackI
              (invoke [this window width height]
                (GL11/glViewport 0 0 width height))))
          (GLFW/glfwSetKeyCallback
            window
            (reify GLFWKeyCallbackI
              (invoke [this window key scan-code action mods]
                (on-keyboard-input window key scan-code action mods))))
          (loop [prev-time (GLFW/glfwGetTime)]
            (when (not (GLFW/glfwWindowShouldClose window))
              (let [curr-time (GLFW/glfwGetTime)]
                (main-loop window (- curr-time prev-time))
                (recur curr-time))))
          (catch Throwable e (println "Exception" e))
          (finally
            (Callbacks/glfwFreeCallbacks window)
            (GLFW/glfwDestroyWindow window)
            (GLFW/glfwTerminate)
            (.free (GLFW/glfwSetErrorCallback nil))
            ))))))

(defn main []
  (run)
  (entity-list-add (new BasicTriangle)))

(comment
  (run)
  (gl-run (GL11/glViewport 0 0 400 400))
  (gl-run (let [[width height] (get-window-size (deref global-window))] (GL11/glViewport 0 0 width height)))
  (gl-run (GLFW/glfwSetKeyCallback
            (deref global-window)
            (reify GLFWKeyCallbackI
              (invoke [this window key scan-code action mods]
                (on-keyboard-input window key scan-code action mods)))))
  )

