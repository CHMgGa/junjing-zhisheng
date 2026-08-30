<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

const props = defineProps({
  twinData: { type: Object, required: true },
  selected: { type: Object, default: () => ({ type: 'greenhouse', id: 'GH-001' }) },
})
const emit = defineEmits(['entity-select'])

const container = ref(null)
let renderer, scene, camera, controls, animationId, resizeObserver, startTime
let raycaster, pointer
const modelNodes = new Map()
const selectableObjects = []

function addModelNode(key, node) {
  if (!modelNodes.has(key)) modelNodes.set(key, [])
  modelNodes.get(key).push(node)
}

function getModelNodes(key) {
  const value = modelNodes.get(key)
  if (!value) return []
  return Array.isArray(value) ? value : [value]
}

function modelKey(greenhouseId, modelNode) {
  return `${greenhouseId}:${modelNode}`
}

function getNodesByModel(modelNode) {
  return Array.from(modelNodes.entries())
    .filter(([key]) => key.endsWith(`:${modelNode}`))
    .flatMap(([, nodes]) => nodes)
}

const WIDTH = 7.2
const LENGTH = 8.8
const WALL_HEIGHT = 1.05
const ROOF_RISE = 2.25

const mat = (color, options = {}) => new THREE.MeshStandardMaterial({
  color,
  roughness: 0.72,
  metalness: 0.05,
  ...options,
})

function random(seed) {
  const value = Math.sin(seed * 982.451) * 43758.5453
  return value - Math.floor(value)
}

function archPoint(progress, z) {
  const angle = -Math.PI / 2 + progress * Math.PI
  return new THREE.Vector3(
    (WIDTH / 2) * Math.sin(angle),
    WALL_HEIGHT + ROOF_RISE * Math.cos(angle),
    z,
  )
}

function setShadows(object) {
  object.traverse((child) => {
    if (child.isMesh) {
      child.castShadow = true
      child.receiveShadow = true
    }
  })
}

function makeSelectable(object, payload) {
  if (!payload?.id) return
  object.userData.selectPayload = payload
  selectableObjects.push(object)
}

function getDevicePayload(modelNode, greenhouseId) {
  const device = props.twinData.devices.find((item) => item.modelNode === modelNode && item.greenhouseId === greenhouseId)
  return device ? { type: 'device', id: device.deviceId } : null
}

function getSensorValue(type, greenhouseId) {
  const sensor = props.twinData.sensors.find((item) => item.type === type && item.greenhouseId === greenhouseId)
  return sensor ? `${sensor.value}${sensor.unit}` : '--'
}

function createTextPlane(text, width = 2.3, height = 0.34, bg = '#1f5d42') {
  const canvas = document.createElement('canvas')
  canvas.width = 1024
  canvas.height = 180
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.fillStyle = '#edf8f1'
  ctx.font = '800 62px Microsoft YaHei, sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(text, canvas.width / 2, canvas.height / 2)
  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  return new THREE.Mesh(new THREE.PlaneGeometry(width, height), new THREE.MeshBasicMaterial({ map: texture }))
}

function createDataBoardTexture(greenhouse) {
  const canvas = document.createElement('canvas')
  canvas.width = 2048
  canvas.height = 900
  const ctx = canvas.getContext('2d')

  ctx.fillStyle = 'rgba(3, 17, 13, 0.98)'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.fillStyle = 'rgba(18, 55, 39, 0.96)'
  ctx.fillRect(28, 28, canvas.width - 56, canvas.height - 56)
  ctx.strokeStyle = greenhouse.status === 'WARNING' ? '#ffc65c' : '#69e6a0'
  ctx.lineWidth = 14
  ctx.strokeRect(18, 18, canvas.width - 36, canvas.height - 36)

  ctx.fillStyle = '#f2fff8'
  ctx.font = '900 94px Microsoft YaHei, sans-serif'
  ctx.textAlign = 'left'
  ctx.fillText(greenhouse.name || greenhouse.id, 82, 142)

  ctx.fillStyle = greenhouse.status === 'WARNING' ? '#ffc65c' : '#69e6a0'
  ctx.font = '800 50px Microsoft YaHei, sans-serif'
  ctx.fillText(greenhouse.status === 'WARNING' ? '需要关注' : '运行正常', 82, 212)

  const rows = [
    ['温度', getSensorValue('TEMPERATURE', greenhouse.id), '湿度', getSensorValue('HUMIDITY', greenhouse.id)],
    ['CO₂', getSensorValue('CO2', greenhouse.id), '光照', getSensorValue('LIGHT', greenhouse.id)],
    ['基质', getSensorValue('SOIL_MOISTURE', greenhouse.id), '阶段', greenhouse.cropStage || '未设置'],
  ]

  rows.forEach((row, index) => {
    const y = 336 + index * 146
    ctx.fillStyle = 'rgba(255, 255, 255, 0.11)'
    ctx.fillRect(78, y - 84, 1888, 102)
    ctx.fillStyle = '#98d6b2'
    ctx.font = '800 48px Microsoft YaHei, sans-serif'
    ctx.fillText(row[0], 120, y - 17)
    ctx.fillText(row[2], 1040, y - 17)
    ctx.fillStyle = '#ffffff'
    ctx.font = '900 64px Microsoft YaHei, sans-serif'
    ctx.fillText(row[1], 300, y - 17)
    ctx.fillText(row[3], 1220, y - 17)
  })

  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  texture.generateMipmaps = false
  texture.minFilter = THREE.LinearFilter
  texture.magFilter = THREE.LinearFilter
  return texture
}

function createDataBoardSprite(greenhouse) {
  const texture = createDataBoardTexture(greenhouse)
  const sprite = new THREE.Sprite(new THREE.SpriteMaterial({
    map: texture,
    transparent: true,
    depthTest: false,
    depthWrite: false,
  }))
  sprite.name = `DATA_BOARD_${greenhouse.id}`
  sprite.position.set(0, 5.05, 0)
  sprite.scale.set(7.4, 3.25, 1)
  sprite.renderOrder = 100
  makeSelectable(sprite, { type: 'greenhouse', id: greenhouse.id })
  addModelNode(sprite.name, sprite)
  return sprite
}

function updateDataBoards() {
  if (!scene) return
  props.twinData.greenhouses.forEach((greenhouse) => {
    const sprite = getModelNodes(`DATA_BOARD_${greenhouse.id}`)[0]
    if (!sprite) return
    const oldTexture = sprite.material.map
    sprite.material.map = createDataBoardTexture(greenhouse)
    sprite.material.needsUpdate = true
    oldTexture?.dispose()
  })
}

function createGreenhouseShell(greenhouse) {
  const group = new THREE.Group()
  group.name = greenhouse.id
  group.position.set(greenhouse.position[0], 0, greenhouse.position[1])

  const steel = mat(0xc5d0cb, { metalness: 0.78, roughness: 0.28 })
  const frame = new THREE.Group()
  for (let z = -LENGTH / 2; z <= LENGTH / 2 + 0.01; z += 1.35) {
    const points = Array.from({ length: 23 }, (_, i) => archPoint(i / 22, z))
    frame.add(new THREE.Mesh(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(points), 60, 0.03, 8), steel))
    for (const x of [-WIDTH / 2, WIDTH / 2]) {
      const post = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.035, WALL_HEIGHT, 8), steel)
      post.position.set(x, WALL_HEIGHT / 2, z)
      frame.add(post)
    }
  }
  for (const progress of [0, 0.25, 0.5, 0.75, 1]) {
    const point = archPoint(progress, 0)
    const rail = new THREE.Mesh(new THREE.CylinderGeometry(0.027, 0.027, LENGTH, 8), steel)
    rail.rotation.x = Math.PI / 2
    rail.position.set(point.x, point.y, 0)
    frame.add(rail)
  }
  group.add(frame)

  const filmMaterial = new THREE.MeshPhysicalMaterial({
    color: greenhouse.status === 'WARNING' ? 0xffefc6 : 0xd9eee4,
    roughness: 0.42,
    transmission: 0.28,
    transparent: true,
    opacity: 0.34,
    side: THREE.DoubleSide,
    depthWrite: false,
  })
  const vertices = []
  const indices = []
  const profileSegments = 32
  const lengthSegments = 8
  for (let zi = 0; zi <= lengthSegments; zi++) {
    const z = -LENGTH / 2 + (zi / lengthSegments) * LENGTH
    for (let pi = 0; pi <= profileSegments; pi++) {
      const p = archPoint(pi / profileSegments, z)
      vertices.push(p.x, p.y, p.z)
    }
  }
  for (let zi = 0; zi < lengthSegments; zi++) {
    for (let pi = 0; pi < profileSegments; pi++) {
      const a = zi * (profileSegments + 1) + pi
      const b = a + profileSegments + 1
      indices.push(a, b, a + 1, b, b + 1, a + 1)
    }
  }
  const filmGeometry = new THREE.BufferGeometry()
  filmGeometry.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3))
  filmGeometry.setIndex(indices)
  filmGeometry.computeVertexNormals()
  const filmMesh = new THREE.Mesh(filmGeometry, filmMaterial)
  filmMesh.name = 'greenhouseFilm'
  group.add(filmMesh)
  addModelNode(modelKey(greenhouse.id, 'SHADE_FILM'), filmMesh)

  const floor = new THREE.Mesh(new THREE.PlaneGeometry(WIDTH - 0.2, LENGTH - 0.2), mat(0x6a5942))
  floor.rotation.x = -Math.PI / 2
  floor.position.y = 0.01
  group.add(floor)

  const door = new THREE.Mesh(new THREE.BoxGeometry(1.35, 1.65, 0.08), mat(0xa9c9bb, { transparent: true, opacity: 0.34 }))
  door.position.set(0, 0.84, LENGTH / 2 + 0.04)
  group.add(door)

  const shortName = greenhouse.name?.replace('羊肚菌大棚', '棚').replace('羊肚菌智能大棚', '智能棚') || greenhouse.id
  const sign = createTextPlane(shortName, 2.6, 0.36, greenhouse.status === 'WARNING' ? '#7a5a18' : '#1f5d42')
  sign.position.set(0, 2.55, LENGTH / 2 + 0.09)
  group.add(sign)

  makeSelectable(floor, { type: 'greenhouse', id: greenhouse.id })
  makeSelectable(door, { type: 'greenhouse', id: greenhouse.id })
  setShadows(group)
  return group
}

function createMushroom(group, x, z, seed) {
  const scale = 0.68 + random(seed) * 0.5
  const stem = new THREE.Mesh(new THREE.CylinderGeometry(0.038 * scale, 0.06 * scale, 0.24 * scale, 8), mat(0xe5d5b6))
  stem.position.set(x, 0.45 + 0.12 * scale, z)
  const cap = new THREE.Mesh(new THREE.ConeGeometry(0.1 * scale, 0.24 * scale, 9), mat(0x7d4b2c))
  cap.position.set(x, 0.63 + 0.21 * scale, z)
  group.add(stem, cap)
}

function createPlantBeds(group, seedOffset = 0) {
  const bedPositions = [-2.55, -0.85, 0.85, 2.55]
  bedPositions.forEach((x, bedIndex) => {
    const bed = new THREE.Mesh(new THREE.BoxGeometry(1.18, 0.34, LENGTH - 1.15), mat(0x59412f))
    bed.position.set(x, 0.21, 0)
    const soil = new THREE.Mesh(new THREE.BoxGeometry(1.08, 0.09, LENGTH - 1.28), mat(0x2f2118))
    soil.position.set(x, 0.42, 0)
    group.add(bed, soil)
    for (let row = 0; row < 3; row++) {
      for (let index = 0; index < 12; index++) {
        const seed = seedOffset + bedIndex * 100 + row * 18 + index
        createMushroom(group, x - 0.35 + row * 0.35 + (random(seed) - 0.5) * 0.1, -3.4 + index * 0.58 + (random(seed + 1) - 0.5) * 0.14, seed)
      }
    }
  })
  const aisle = new THREE.Mesh(new THREE.BoxGeometry(0.72, 0.055, LENGTH - 0.6), mat(0x9a8c72))
  aisle.position.set(0, 0.05, 0)
  group.add(aisle)
}

function createIrrigation(group, greenhouseId) {
  const pipeMaterial = mat(0x1d2c29, { metalness: 0.2, roughness: 0.5 })
  const droplets = new THREE.Group()
  droplets.name = `WATER_SPRAY_${greenhouseId}`
  const pumpPayload = getDevicePayload('PUMP_01', greenhouseId)
  for (const x of [-2.55, -0.85, 0.85, 2.55]) {
    const pipe = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.022, LENGTH - 1.2, 10), pipeMaterial)
    pipe.rotation.x = Math.PI / 2
    pipe.position.set(x, 0.92, 0)
    makeSelectable(pipe, pumpPayload)
    group.add(pipe)
    for (let z = -3.1; z <= 3.1; z += 2.05) {
      const nozzle = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.017, 0.1, 8), pipeMaterial)
      nozzle.position.set(x, 0.7, z)
      makeSelectable(nozzle, pumpPayload)
      group.add(nozzle)
      for (let i = 0; i < 5; i++) {
        const drop = new THREE.Mesh(new THREE.SphereGeometry(0.018, 6, 6), mat(0x70c9f0, { transparent: true, opacity: 0.68 }))
        drop.position.set(x + (i - 2) * 0.06, 0.58 - i * 0.07, z)
        drop.userData.baseY = drop.position.y
        drop.userData.phase = i * 0.7 + z
        droplets.add(drop)
      }
    }
  }
  droplets.visible = false
  group.add(droplets)
  addModelNode(modelKey(greenhouseId, 'WATER_SPRAY'), droplets)
}

function createFan(group, greenhouseId) {
  const fan = new THREE.Group()
  fan.name = 'FAN_01'
  const darkMetal = mat(0x263b37, { metalness: 0.72, roughness: 0.32 })
  const ring = new THREE.Mesh(new THREE.TorusGeometry(0.46, 0.055, 12, 32), darkMetal)
  const center = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.1, 0.14, 18), darkMetal)
  center.rotation.x = Math.PI / 2
  const blades = new THREE.Group()
  blades.name = 'blades'
  for (let i = 0; i < 5; i++) {
    const pivot = new THREE.Group()
    pivot.rotation.z = i * Math.PI * 0.4
    const blade = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.38, 0.04), mat(0x557d6f))
    blade.position.y = 0.22
    pivot.add(blade)
    blades.add(pivot)
  }
  fan.add(ring, center, blades)
  fan.position.set(0, 1.88, -LENGTH / 2 + 0.08)
  makeSelectable(ring, getDevicePayload('FAN_01', greenhouseId))
  group.add(fan)
  addModelNode(modelKey(greenhouseId, 'FAN_01'), fan)
}

function createPumpAndTank(group, greenhouseId) {
  const pumpGroup = new THREE.Group()
  pumpGroup.name = 'PUMP_01'
  const tank = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.32, 0.88, 22), mat(0x7ca6a0, { metalness: 0.22 }))
  tank.position.y = 0.48
  const pump = new THREE.Mesh(new THREE.BoxGeometry(0.48, 0.34, 0.42), mat(0x256c91, { emissive: 0x07243a, emissiveIntensity: 0.15 }))
  pump.position.set(0.48, 0.2, 0)
  const rotor = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 0.08, 16), mat(0x70c9f0, { emissive: 0x0b5d88, emissiveIntensity: 0.2 }))
  rotor.name = 'pumpRotor'
  rotor.rotation.z = Math.PI / 2
  rotor.position.set(0.48, 0.22, 0.24)
  const statusLight = new THREE.Mesh(new THREE.SphereGeometry(0.045, 12, 12), mat(0x50625c, { emissive: 0x000000, emissiveIntensity: 0 }))
  statusLight.name = 'pumpStatusLight'
  statusLight.position.set(0.32, 0.41, 0.22)
  pumpGroup.add(tank, pump, rotor, statusLight)
  pumpGroup.position.set(-3, 0, 3.35)
  for (const item of [tank, pump, rotor, statusLight]) makeSelectable(item, getDevicePayload('PUMP_01', greenhouseId))
  group.add(pumpGroup)
  addModelNode(modelKey(greenhouseId, 'PUMP_01'), pumpGroup)
}

function createLightsAndShade(group, greenhouseId) {
  const lightGroup = new THREE.Group()
  lightGroup.name = 'LAMP_01'
  for (const x of [-1.8, 0, 1.8]) {
    const bar = new THREE.Mesh(new THREE.BoxGeometry(0.1, 0.07, 6.2), mat(0xffe7a1, { emissive: 0xffbd55, emissiveIntensity: 1.8 }))
    bar.position.set(x, 2.42, 0)
    makeSelectable(bar, getDevicePayload('LAMP_01', greenhouseId))
    lightGroup.add(bar)
    const glow = new THREE.PointLight(0xffd48a, 0.48, 3.4, 2)
    glow.position.set(x, 2.25, 0)
    lightGroup.add(glow)
  }
  group.add(lightGroup)
  addModelNode(modelKey(greenhouseId, 'LAMP_01'), lightGroup)

  const shade = new THREE.Group()
  shade.name = 'SHADE_01'
  for (const x of [-3.15, 3.15]) {
    const roll = new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.09, LENGTH - 0.8, 14), mat(0x304e43))
    roll.rotation.x = Math.PI / 2
    roll.position.set(x, 1.36, 0)
    makeSelectable(roll, getDevicePayload('SHADE_01', greenhouseId))
    shade.add(roll)
  }
  group.add(shade)
  addModelNode(modelKey(greenhouseId, 'SHADE_01'), shade)
}

function createSensorNode(group, sensor, x, z, color) {
  const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.02, 0.02, 1.1, 8), mat(0x52655d))
  pole.position.set(x, 0.56, z)
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.28, 0.16), mat(0xe3eee9))
  body.position.set(x, 1.08, z)
  const indicator = new THREE.Mesh(new THREE.SphereGeometry(0.045, 12, 12), mat(color, { emissive: color, emissiveIntensity: 0.65 }))
  indicator.position.set(x, 1.13, z + 0.09)
  const hitArea = new THREE.Mesh(new THREE.SphereGeometry(0.32, 12, 12), new THREE.MeshBasicMaterial({ transparent: true, opacity: 0, depthWrite: false }))
  hitArea.position.set(x, 1.1, z)
  for (const item of [body, indicator, hitArea]) makeSelectable(item, { type: 'sensor', id: sensor.deviceId })
  group.add(pole, body, indicator, hitArea)
  addModelNode(sensor.modelNode, body)
}

function createSensorsAndCamera(group, greenhouseId) {
  const positions = {
    SENSOR_TEMP_01: [-1.8, -1.9, 0x43c984],
    SENSOR_HUM_01: [1.8, -1.2, 0x4db8ff],
    SENSOR_CO2_01: [-2.4, 2.9, 0xffc65c],
    SENSOR_LIGHT_01: [0, 0.2, 0xffdf70],
    SENSOR_SOIL_01: [2.2, 2.3, 0xff9d63],
  }
  props.twinData.sensors.filter((sensor) => sensor.greenhouseId === greenhouseId).forEach((sensor) => {
    const position = positions[sensor.modelNode]
    if (position) createSensorNode(group, sensor, position[0], position[1], position[2])
  })

  const cameraGroup = new THREE.Group()
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.2, 0.36), mat(0xe3eee9))
  const lens = new THREE.Mesh(new THREE.CylinderGeometry(0.075, 0.075, 0.11, 18), mat(0x101817))
  lens.rotation.x = Math.PI / 2
  lens.position.z = -0.22
  cameraGroup.add(body, lens)
  cameraGroup.position.set(2.35, 2.18, 2.15)
  cameraGroup.rotation.set(-0.18, -2.45, 0)
  group.add(cameraGroup)

  const cabinet = new THREE.Mesh(new THREE.BoxGeometry(0.52, 0.92, 0.3), mat(0xc7d0cc))
  cabinet.position.set(3.1, 0.48, 3.3)
  group.add(cabinet)
}

function createGreenhouse(greenhouse, index) {
  const group = createGreenhouseShell(greenhouse)
  group.add(createDataBoardSprite(greenhouse))
  createPlantBeds(group, index * 1000)
  createIrrigation(group, greenhouse.id)
  createFan(group, greenhouse.id)
  createPumpAndTank(group, greenhouse.id)
  createLightsAndShade(group, greenhouse.id)
  createSensorsAndCamera(group, greenhouse.id)
  scene.add(group)
}

function createEnvironment() {
  scene.background = new THREE.Color(0x9ec4b7)
  scene.fog = new THREE.Fog(0x9ec4b7, 55, 115)
  const outside = new THREE.Mesh(new THREE.PlaneGeometry(42, 42), mat(0x526a4d))
  outside.rotation.x = -Math.PI / 2
  outside.position.y = -0.04
  scene.add(outside)
  const roadMaterial = mat(0x948b78)
  const verticalRoad = new THREE.Mesh(new THREE.PlaneGeometry(2.6, 36), roadMaterial)
  verticalRoad.rotation.x = -Math.PI / 2
  verticalRoad.position.y = 0.005
  scene.add(verticalRoad)
  const horizontalRoad = new THREE.Mesh(new THREE.PlaneGeometry(36, 2.6), roadMaterial)
  horizontalRoad.rotation.x = -Math.PI / 2
  horizontalRoad.position.y = 0.006
  scene.add(horizontalRoad)
}

function buildScene() {
  scene = new THREE.Scene()
  startTime = Date.now()
  modelNodes.clear()
  selectableObjects.length = 0
  raycaster = new THREE.Raycaster()
  pointer = new THREE.Vector2()

  camera = new THREE.PerspectiveCamera(44, 1, 0.1, 220)
  camera.position.set(36, 28, 44)
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.04
  container.value.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.055
  controls.target.set(0, 1.1, 0)
  controls.maxPolarAngle = Math.PI / 2.04
  controls.minDistance = 8
  controls.maxDistance = 115

  scene.add(new THREE.HemisphereLight(0xe3fff3, 0x536246, 2.5))
  const sun = new THREE.DirectionalLight(0xfff5df, 3.8)
  sun.position.set(-14, 20, 12)
  sun.castShadow = true
  sun.shadow.mapSize.set(2048, 2048)
  sun.shadow.camera.left = -24
  sun.shadow.camera.right = 24
  sun.shadow.camera.top = 24
  sun.shadow.camera.bottom = -24
  scene.add(sun)

  createEnvironment()
  props.twinData.greenhouses.forEach(createGreenhouse)
  updateDeviceState()
  updateSelection()
}

function updateDeviceState() {
  if (!scene) return
  props.twinData.devices.forEach((device) => {
    const nodes = getModelNodes(modelKey(device.greenhouseId, device.modelNode))
    if (!nodes.length) return
    const running = device.runningStatus === 'ON'

    nodes.forEach((node) => {
      node.userData.running = running
      if (device.modelNode === 'PUMP_01') {
        getModelNodes(modelKey(device.greenhouseId, 'WATER_SPRAY')).forEach((spray) => { spray.visible = running })
        const statusLight = node.getObjectByName('pumpStatusLight')
        if (statusLight?.material?.emissive) {
          statusLight.material.color.setHex(running ? 0x70e6ff : 0x50625c)
          statusLight.material.emissive.setHex(running ? 0x1eb9ff : 0x000000)
          statusLight.material.emissiveIntensity = running ? 1.6 : 0
        }
      }
      if (device.modelNode === 'LAMP_01') {
        node.traverse((child) => {
          if (child.isLight) child.intensity = running ? 0.48 : 0
          if (child.isMesh && child.material.emissive) child.material.emissiveIntensity = running ? 1.8 : 0.05
        })
      }
      if (device.modelNode === 'SHADE_01') {
        getModelNodes(modelKey(device.greenhouseId, 'SHADE_FILM')).forEach((film) => {
          if (!film.material) return
          film.material.color.setHex(running ? 0x6f806f : 0xd9eee4)
          film.material.opacity = running ? 0.55 : 0.34
          film.material.roughness = running ? 0.86 : 0.42
          film.material.transmission = running ? 0.08 : 0.28
          film.material.needsUpdate = true
        })
      }
    })
  })
}

function updateSelection() {
  if (!scene) return
  const selectedObject = selectableObjects.find((object) => {
    const payload = object.userData.selectPayload
    return payload?.type === props.selected?.type && payload?.id === props.selected?.id
  })
  if (selectedObject?.material?.emissive) {
    selectedObject.material.emissive.setHex(0x8cffb0)
    selectedObject.material.emissiveIntensity = 0.9
  }
}

function getSelectableFromIntersection(object) {
  let current = object
  while (current) {
    if (current.userData.selectPayload) return current
    current = current.parent
  }
  return null
}

function handleClick(event) {
  const rect = renderer.domElement.getBoundingClientRect()
  pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(pointer, camera)
  const intersections = raycaster.intersectObjects(selectableObjects, true)
  const picked = intersections.length ? getSelectableFromIntersection(intersections[0].object) : null
  if (picked?.userData.selectPayload) emit('entity-select', picked.userData.selectPayload)
}

function resize() {
  if (!container.value || !renderer) return
  const { clientWidth, clientHeight } = container.value
  renderer.setSize(clientWidth, clientHeight, false)
  camera.aspect = clientWidth / clientHeight
  camera.updateProjectionMatrix()
}

function animate() {
  animationId = requestAnimationFrame(animate)
  const elapsed = (Date.now() - startTime) / 1000
  getNodesByModel('FAN_01').forEach((fan) => {
    if (fan?.userData.running) fan.getObjectByName('blades').rotation.z -= 0.12
  })
  getNodesByModel('PUMP_01').forEach((pump) => {
    if (!pump?.userData.running) return
    const rotor = pump.getObjectByName('pumpRotor')
    const statusLight = pump.getObjectByName('pumpStatusLight')
    if (rotor) rotor.rotation.x += 0.22
    if (statusLight) statusLight.scale.setScalar(1 + Math.sin(elapsed * 8) * 0.12)
  })
  getNodesByModel('WATER_SPRAY').forEach((spray) => {
    if (!spray?.visible) return
    spray.children.forEach((drop) => {
      drop.position.y = drop.userData.baseY - ((elapsed * 0.48 + drop.userData.phase) % 0.42)
    })
  })
  controls.update()
  renderer.render(scene, camera)
}

function disposeScene() {
  scene?.traverse((object) => {
    if (object.geometry) object.geometry.dispose()
    if (object.material) {
      const materials = Array.isArray(object.material) ? object.material : [object.material]
      materials.forEach((item) => item.dispose())
    }
  })
}

watch(() => props.twinData.devices, updateDeviceState, { deep: true })
watch(() => props.twinData.sensors, updateDataBoards, { deep: true })
watch(() => props.twinData.greenhouses, updateDataBoards, { deep: true })
watch(() => props.selected, updateSelection, { deep: true })

onMounted(() => {
  buildScene()
  resize()
  animate()
  renderer.domElement.addEventListener('click', handleClick)
  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(container.value)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animationId)
  resizeObserver?.disconnect()
  renderer?.domElement.removeEventListener('click', handleClick)
  controls?.dispose()
  disposeScene()
  renderer?.dispose()
  renderer?.domElement.remove()
})
</script>

<template><div ref="container" class="scene-container" /></template>

<style scoped>
.scene-container { width: 100%; cursor: grab; background: #9ec4b7; }
.scene-container:active { cursor: grabbing; }
</style>
