(ns game.shaders
  (:require [clojure.java.io :as io])
  (:use [game.task-queue :only [gl-run]])
  (:use [game.core :only [main entity-list-add entity-list-set entity-list-clear]])
  (:import
    (org.lwjgl.opengl GL GL11 GL20 GL15 GL30 GL13)
    (java.awt.image BufferedImage)
    (javax.imageio ImageIO)
    (game.core Entity)
    (org.joml Matrix4f Vector4f Vector3f Vector3fc)
    (java.nio ByteBuffer FloatBuffer)
    (org.lwjgl BufferUtils)
    (org.lwjgl.glfw GLFW)))



(defn byte-buffer [bytes]
  (let [buf (BufferUtils/createByteBuffer (count bytes))]
    (.put buf bytes)
    (.flip buf)
    buf))

(defn create-float-buffer [size]
  (BufferUtils/createFloatBuffer size))

(defprotocol GLResource
  (gl-id ^Integer [this]))

(defmacro def-gl-type [name create-fn dispose-fn]
  `(do
     (deftype ~name [id#]
       Object
       (finalize [this#]
         (gl-run
           (println "Deleting" ~(str name) id#)
           (~dispose-fn id#)))
       GLResource
       (gl-id [this#] id#))
     (defn ~(symbol (str "create-" (clojure.string/lower-case name))) [& rest#]
       (new ~name (apply ~create-fn rest#)))))

(def-gl-type
  GLBuffer
  #(GL15/glGenBuffers)
  GL15/glDeleteBuffers)

(def-gl-type
  GLProgram
  #(GL20/glCreateProgram)
  GL20/glDeleteProgram)

(defn use-program [program]
  (GL20/glUseProgram (gl-id program)))

(def-gl-type
  GLShader
  (fn [type] (GL20/glCreateShader type))
  GL20/glDeleteShader)

(def-gl-type
  GLVertexArray
  #(GL30/glGenVertexArrays)
  GL30/glDeleteVertexArrays)

(def-gl-type
  GLTexture
  #(GL11/glGenTextures)
  GL11/glDeleteTextures)

(defn compile-program [shaders]
  (let [program (create-glprogram)]
    (doall (for [shader shaders]
             (GL20/glAttachShader (gl-id program) (gl-id shader))))
    (GL20/glLinkProgram (gl-id program))
    (when (= GL11/GL_FALSE (GL20/glGetProgrami (gl-id program) GL20/GL_LINK_STATUS))
      (throw (RuntimeException.
               (str "Unable to link program: (" (GL11/glGetError) ")" (GL20/glGetProgramInfoLog (gl-id program) 40960)))))
    program))

(defn compile-shader [^String filename ^Integer type]
  (let [shader (create-glshader type)
        shader-code (slurp (io/resource filename))]
    (println "Compiling " shader-code)
    (GL20/glShaderSource (gl-id shader) shader-code)
    (GL20/glCompileShader (gl-id shader))
    (when (= GL11/GL_FALSE (GL20/glGetShaderi (gl-id shader) GL20/GL_COMPILE_STATUS))
      (throw (RuntimeException.
               (str "Unable to compile program: " (GL20/glGetShaderInfoLog (gl-id shader) 4096)))))
    shader))

(defn get-uniform-location [^GLProgram program ^String uniform-name]
  (GL20/glGetUniformLocation (gl-id program) uniform-name))

(defn set-uniform-mat4-matrix [^Integer location ^FloatBuffer fb]
  "Set uniform in column major order - columns are contiguous"
  (when (not (= -1 location))
      (GL20/glUniformMatrix4fv location false fb)))

(defn load-image-resource [^String resource-path]
  (ImageIO/read (io/resource resource-path)))

(defrecord Image [^Integer width ^Integer height ^ByteBuffer rgba])

(defn ^Image create-image [^BufferedImage image]
  (let [
        width (.getWidth image)
        height (.getHeight image)
        rgbRaster (.getRaster image)
        alphaRaster (.getAlphaRaster image)
        bytes
        (for [y (range height)
              x (range width)]
          [
           (.getSample rgbRaster x (- height y 1) 0)
           (.getSample rgbRaster x (- height y 1) 1)
           (.getSample rgbRaster x (- height y 1) 2)
           (if alphaRaster
             (.getSample alphaRaster x (- height y 1) 2)
             255)
           ])]
    (new Image width height (-> bytes flatten byte-array byte-buffer))
    ))

(defn ^GLTexture create-texture [^Image image]
  (let [{:keys [^Integer width ^Integer height ^ByteBuffer rgba]} image
        texture (create-gltexture)]
    (GL11/glBindTexture GL11/GL_TEXTURE_2D (gl-id texture))
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MIN_FILTER GL11/GL_LINEAR)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MAG_FILTER GL11/GL_LINEAR)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_WRAP_S GL11/GL_REPEAT)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_WRAP_T GL11/GL_REPEAT)
    (GL11/glTexImage2D GL11/GL_TEXTURE_2D 0 GL11/GL_RGBA width height 0 GL11/GL_RGBA GL11/GL_UNSIGNED_BYTE rgba)
    texture
    ))

(defrecord Mesh
  [positions normals uv triangles texture])

(defn draw-elements [^GLBuffer elements-array ^Integer num-elements]
  (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER (gl-id elements-array))
  (GL11/glDrawElements GL11/GL_TRIANGLES num-elements GL11/GL_UNSIGNED_INT 0))

(defn bind-texture [^GLTexture texture texture-index]
  (GL13/glActiveTexture (+ GL13/GL_TEXTURE0 texture-index))
  (GL11/glBindTexture GL11/GL_TEXTURE_2D (gl-id texture)))

(defrecord SimpleRendererBindings
  [view-tr-binding model-tr-binding pos-binding normal-binding uv-binding])

(defrecord SimpleRenderer
  [^GLProgram program ^SimpleRendererBindings bindings])

(defrecord SimpleCompiledMesh
  [^Matrix4f model-tr ^GLVertexArray vertex-array ^GLBuffer elements-array
   ^GLTexture texture buffers ^Integer num-elements ^SimpleRenderer renderer])

(defn bind-attribute-array [binding]
  (when (not (= -1 binding))
    (GL20/glEnableVertexAttribArray binding)))

(defn render-simple-compiled-mesh
  [^Matrix4f view-tr ^Matrix4f model-tr ^SimpleCompiledMesh mesh]
  (let [{:keys [:renderer :elements-array :num-elements :texture :vertex-array]} mesh
        {:keys [:program :bindings]} renderer
        {:keys [:view-tr-binding :model-tr-binding
                :pos-binding :normal-binding
                :uv-binding]} bindings]
    (use-program (:program renderer))
    (set-uniform-mat4-matrix view-tr-binding view-tr)
    (set-uniform-mat4-matrix model-tr-binding model-tr)
    (GL11/glPolygonMode GL11/GL_FRONT_AND_BACK GL11/GL_FILL)
    (bind-attribute-array pos-binding)
    (bind-attribute-array normal-binding)
    (bind-attribute-array uv-binding)
    (bind-texture texture 0)
    (draw-elements elements-array num-elements)))

(defrecord SimpleScene
  [^FloatBuffer view-fb
   ^FloatBuffer model-fb
   ^Matrix4f projection-view-tr
   ^Matrix4f projection-tr
   ^Matrix4f view-tr
   ^Matrix4f model-tr
   ^SimpleCompiledMesh mesh
   controllers]
  Entity
  (render-entity [self window]
    (-> projection-view-tr .identity (.mul projection-tr) (.mul view-tr) (.get view-fb))
    (-> model-tr (.get model-fb))
    (render-simple-compiled-mesh view-fb model-fb mesh))
  (update-entity [self window delta-time] (doall (map #(% self window delta-time) (deref controllers)))))

(defn create-simple-scene [projection-tr view-tr model-tr mesh]
  (SimpleScene. (create-float-buffer 16) (create-float-buffer 16) (new Matrix4f) projection-tr view-tr model-tr mesh (ref [])))

(defn get-attribute-location [^GLProgram program ^String name]
  (GL20/glGetAttribLocation (gl-id program) name))

(defn create-simple-renderer []
  (let [
        shaders [
                 (compile-shader "simple-vert.glsl" GL20/GL_VERTEX_SHADER)
                 (compile-shader "simple-frag.glsl" GL20/GL_FRAGMENT_SHADER)
                 ]
        program (compile-program shaders)
        bindings-map {
                      :view-tr-binding  (get-uniform-location program "view_tr")
                      :model-tr-binding (get-uniform-location program "model_tr")
                      :pos-binding      (get-attribute-location program "positions")
                      :normal-binding   (get-attribute-location program "normals")
                      :uv-binding       (get-attribute-location program "uv")
                      }
        bindings (map->SimpleRendererBindings bindings-map)
        ]
    (SimpleRenderer. program bindings)))

(defn bind-vertex-attributes [^GLVertexArray vertex-array ^Integer values-per-vertex ^Integer vertex-binding #^Byte data]
  (let [buffer (create-glbuffer)
        buffer-type GL15/GL_ARRAY_BUFFER]
    (GL30/glBindVertexArray (gl-id vertex-array))
    (GL15/glBindBuffer buffer-type (gl-id buffer))
    (GL15/glBufferData buffer-type data GL15/GL_STATIC_DRAW)
    (GL20/glVertexAttribPointer vertex-binding values-per-vertex GL11/GL_FLOAT false 0 0)
    buffer))

(defn create-elements-array [#^Byte data]
  (let [buffer (create-glbuffer)
        buffer-type GL15/GL_ELEMENT_ARRAY_BUFFER]
    (println "Binding elements with" (count data) "elements to buffer" (gl-id buffer))
    (GL15/glBindBuffer buffer-type (gl-id buffer))
    (GL15/glBufferData buffer-type data GL15/GL_STATIC_DRAW)

    (GL15/glBindBuffer buffer-type 0)
    buffer))

(defn ^SimpleCompiledMesh create-simple-compiled-mesh [^SimpleRenderer renderer ^Mesh mesh]
  (let
    [{:keys [#^Float positions #^Float normals #^Float uv #^Integer triangles]} mesh
     vertex-array (create-glvertexarray)
     elements-array (create-elements-array triangles)
     texture (:texture mesh)
     bindings (:bindings renderer)
     num-elements (count triangles)
     bind-va
     (fn [attribute values-per-vertex data]
       (let [vertex-binding (attribute bindings)]
         (when (not (= -1 vertex-binding))
           (println "Binding" attribute "at" vertex-binding "to vertex-array" (gl-id vertex-array))
           (bind-vertex-attributes vertex-array values-per-vertex vertex-binding data))))]
    (map->SimpleCompiledMesh {
                              :renderer       renderer
                              :vertex-array   vertex-array
                              :elements-array elements-array
                              :texture        texture
                              :buffers        [
                                               (bind-va :pos-binding 3 positions)
                                               (bind-va :normal-binding 3 normals)
                                               (bind-va :uv-binding 2 uv)
                                               ]
                              :num-elements   num-elements})))

(defn ^Mesh create-mesh [args-map]
  (let [{:keys [positions normals uv triangles texture]} args-map]
    (map->Mesh
      {
       :positions (float-array positions)
       :normals   (float-array normals)
       :uv        (float-array uv)
       :triangles (int-array triangles)
       :texture   texture})))

(defn mat4-frustum [^Float left ^Float right ^Float bottom ^Float top ^Float near ^Float far]
  (-> (new Matrix4f) (.frustum left right bottom top near far)))

(defn mat4-scale [^Float x ^Float y ^Float z]
  (-> (new Matrix4f) (.scale x y z)))

(defn mat4-scale [^Float x ^Float y ^Float z]
  (-> (new Matrix4f) (.scale x y z)))

(defn mat4-ident [] (new Matrix4f))

(defn scene-translate [^SimpleScene scene ^Vector3fc translation]
  (-> (:view-tr scene) (.translate translation)))

(defn scene-model-rotate [^SimpleScene scene ^Float angle]
  (-> (:model-tr scene) (.rotate angle 0 0 1)))

(defn scene-reset-view-tr [^SimpleScene scene ^Vector3fc translation]
  (-> (:view-tr scene) (.identity)))

(defn scene-add-controller [^SimpleScene scene controller]
  (let [controllers (:controllers scene)]
    (dosync (ref-set controllers (conj (deref controllers) controller)))))

(defn scene-clear-controllers [^SimpleScene scene]
  (let [controllers (:controllers scene)]
    (dosync (ref-set controllers []))))

(defn scene-set-controller [^SimpleScene scene controller]
  (let [controllers (:controllers scene)]
    (dosync (ref-set controllers [controller]))))

(defn vec3 [^Float x ^Float y ^Float z]
  (new Vector3f x y z))

(defn key-down? [window key]
  (= (GLFW/glfwGetKey window key) GLFW/GLFW_PRESS))

(defn move-controller [scene window delta-time]
  (let [d delta-time]
    (when (key-down? window GLFW/GLFW_KEY_A)
      (scene-translate scene (vec3 d 0 0)))
    (when (key-down? window GLFW/GLFW_KEY_W)
      (scene-translate scene (vec3 0 0 d)))
    (when (key-down? window GLFW/GLFW_KEY_D)
      (scene-translate scene (vec3 (- d) 0 0)))
    (when (key-down? window GLFW/GLFW_KEY_S)
      (scene-translate scene (vec3 0 0 (- d))))))

(defn rotation-controller [scene window delta-time]
    (scene-model-rotate scene (/ delta-time (* 2.0 Math/PI))))

(comment
    (game.core/main)

    (do
    (def stone-texture-image
      (-> "stone_texture.jpg" load-image-resource create-image))

    (def stone-texture
      (gl-run (create-texture stone-texture-image)))

    (def triangle-mesh
      (create-mesh
        {
         :positions [0.0 0.0 -2
                     0.5 0.0 -2
                     0.5 0.5 -2]
         :normals   [0 0 1
                     0 0 1
                     0 0 1]
         :uv        [0 0
                     0.5 0
                     0.5 0.5]
         :triangles [0 1 2]
         :texture   stone-texture}))

    (def simple-renderer (gl-run (create-simple-renderer)))
    (def compiled-triangle-mesh (gl-run (create-simple-compiled-mesh simple-renderer triangle-mesh)))

    (def simple-scene
      (let
        [
         projection-tr (mat4-frustum -1 1 -1 1 1 1000)
         view-tr (mat4-ident)
         model-tr (mat4-scale 4 4 1)
         ]
        (gl-run (create-simple-scene projection-tr view-tr model-tr compiled-triangle-mesh))))

    (entity-list-set simple-scene)
    )

  (scene-add-controller simple-scene move-controller)
  (scene-add-controller simple-scene rotation-controller)
  (scene-add-controller simple-scene (fn [scene] (println "Update scene")))
  (scene-clear-controllers simple-scene)

  (scene-translate simple-scene (vec3 0 0 -0.5))

  (entity-list-clear)
  (gl-run (GL20/glUseProgram (-> simple-renderer :program gl-id)))
  (gl-run (gl-id (:program simple-renderer)))
  (gl-run (game.core/use-basic-shape (gl-id (:program simple-renderer))))
  (gl-run (game.core/basic-init 3))
  (gl-run (game.core/set-scene! (new game.core.BasicShape 1)))
  (gl-run (-> simple-renderer :program (get-attribute-location "normals")))
  )