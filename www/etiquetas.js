/* Diccionario inglés → castellano de las etiquetas que ML Kit anota en cada foto.
   Cada línea: etiqueta_en | nombre que se muestra, sinónimos que también se buscan.
   Las fotos ya indexadas NO se vuelven a analizar: se traduce al buscar y al mostrar. */
(function(){
const L = `
person|persona, gente, alguien
selfie|selfie, autofoto
smile|sonrisa, sonriendo
face|cara, rostro
head|cabeza
hair|pelo, cabello
hairstyle|peinado
long hair|pelo largo
beard|barba
moustache|bigote
facial hair|vello facial
forehead|frente
chin|mentón, barbilla
cheek|mejilla
eyebrow|ceja
eyelash|pestaña
eye|ojo, ojos
nose|nariz
mouth|boca
lip|labio, labios
tooth|diente, dientes
skin|piel
neck|cuello
shoulder|hombro
arm|brazo
hand|mano, manos
finger|dedo, dedos
thumb|pulgar
leg|pierna
thigh|muslo
knee|rodilla
foot|pie, pies
toe|dedo del pie
muscle|músculo
joint|articulación
human body|cuerpo, cuerpo humano
human|persona, humano
abdomen|abdomen, panza
chest|pecho
back|espalda
bride|novia
groom|novio
wedding|casamiento, boda
wedding dress|vestido de novia
gown|vestido largo, vestido de gala
dress|vestido
suit|traje
tie|corbata
shirt|camisa, remera
t-shirt|remera, camiseta
jeans|jean, jeans, vaqueros
trousers|pantalón
shorts|short, bermuda
skirt|pollera, falda
jacket|campera, chaqueta
coat|abrigo, tapado
sweater|pulóver, suéter
hat|sombrero, gorro
cap|gorra
sunglasses|anteojos de sol, lentes de sol
glasses|anteojos, lentes, gafas
scarf|bufanda
glove|guante
sock|media, calcetín
shoe|zapato, zapatilla
footwear|calzado, zapatillas
boot|bota
sandal|sandalia, ojota
bag|bolso, cartera, bolsa
handbag|cartera, bolso
backpack|mochila
luggage and bags|valija, equipaje, bolsos
wallet|billetera
jewellery|joyas, bijouterie
jewelry|joyas, bijouterie
necklace|collar
earring|arito, aro, pendiente
ring|anillo
bracelet|pulsera
watch|reloj
clothing|ropa, vestimenta
fashion|moda
fashion accessory|accesorio, accesorios
costume|disfraz
mask|máscara, barbijo
uniform|uniforme
baby|bebé
child|niño, niña, chico, chica, nene, nena
toddler|bebé, nene pequeño
boy|nene, chico, niño
girl|nena, chica, niña
man|hombre, señor
woman|mujer, señora
senior citizen|abuelo, abuela, adulto mayor
crowd|multitud, gente
team|equipo
family|familia
friendship|amigos, amistad
fun|diversión
party|fiesta
celebration|celebración, festejo
event|evento
birthday|cumpleaños
birthday cake|torta de cumpleaños
cake|torta, pastel
cupcake|cupcake, magdalena
candle|vela
balloon|globo, globos
gift|regalo
christmas|navidad
christmas tree|árbol de navidad
christmas decoration|adorno navideño
halloween|halloween
fireworks|fuegos artificiales
concert|recital, concierto
stage|escenario
performance|show, espectáculo
dance|baile, bailar
dancer|bailarín, bailarina
musician|músico
singer|cantante
guitar|guitarra
guitarist|guitarrista
piano|piano
keyboard|teclado
drum|tambor, batería
violin|violín
microphone|micrófono
musical instrument|instrumento musical, instrumento
music|música
speaker|parlante, altavoz
headphones|auriculares
dog|perro, perrito, mascota
puppy|cachorro, perrito
dog breed|perro, raza de perro
cat|gato, gatito, mascota
kitten|gatito
pet|mascota
animal|animal
mammal|mamífero
bird|pájaro, ave
parrot|loro
duck|pato
chicken|pollo, gallina
rooster|gallo
eagle|águila
owl|búho, lechuza
fish|pez, pescado
shark|tiburón
whale|ballena
dolphin|delfín
turtle|tortuga
snake|serpiente, víbora
lizard|lagarto, lagartija
frog|rana, sapo
insect|insecto, bicho
butterfly|mariposa
bee|abeja
ant|hormiga
spider|araña
horse|caballo
pony|pony, potrillo
cow|vaca
cattle|ganado, vacas
sheep|oveja
goat|cabra
pig|chancho, cerdo
donkey|burro
rabbit|conejo
hamster|hámster
mouse|ratón
squirrel|ardilla
deer|ciervo, venado
bear|oso
lion|león
tiger|tigre
elephant|elefante
giraffe|jirafa
monkey|mono
zebra|cebra
fox|zorro
wolf|lobo
camel|camello
llama|llama
penguin|pingüino
seal|foca
pet supplies|accesorios para mascotas
plant|planta
flower|flor, flores
petal|pétalo
rose|rosa
tulip|tulipán
sunflower|girasol
tree|árbol, árboles
palm tree|palmera
leaf|hoja
grass|pasto, césped
lawn|césped, jardín
shrub|arbusto
bush|arbusto
flowerpot|maceta
houseplant|planta de interior, planta
garden|jardín
bouquet|ramo
cactus|cactus
succulent plant|suculenta
forest|bosque
woodland|bosque
jungle|selva
nature|naturaleza
natural landscape|paisaje
landscape|paisaje
mountain|montaña, cerro
mountainous landforms|montañas, cerros, cordillera
hill|cerro, colina
valley|valle
volcano|volcán
cliff|acantilado, barranca
rock|roca, piedra
stone|piedra
sand|arena
desert|desierto
dune|duna
beach|playa
coast|costa
shore|orilla
sea|mar
ocean|océano, mar
wave|ola
water|agua
lake|lago, laguna
river|río
stream|arroyo
waterfall|cascada, salto de agua
pond|estanque
pool|pileta, piscina
swimming pool|pileta, piscina
reservoir|embalse, dique
dam|dique, represa
canal|canal
bay|bahía
island|isla
horizon|horizonte
sky|cielo
cloud|nube, nubes
sun|sol
sunlight|luz del sol, sol
sunset|atardecer, puesta de sol
sunrise|amanecer
dusk|crepúsculo, anochecer
dawn|amanecer
night|noche
moon|luna
star|estrella
snow|nieve
ice|hielo
winter|invierno
autumn|otoño
spring|primavera
summer|verano
fog|niebla
mist|neblina
rain|lluvia
storm|tormenta
rainbow|arcoíris
lightning|rayo
fire|fuego
flame|llama, fuego
bonfire|fogata
smoke|humo
sand castle|castillo de arena
camping|camping, acampar
tent|carpa
campfire|fogata
hiking|senderismo, caminata
trail|sendero
road|calle, ruta, camino
street|calle
highway|autopista, ruta
asphalt|asfalto
road surface|asfalto, camino
path|camino, sendero
pavement|vereda, pavimento
sidewalk|vereda
bridge|puente
tunnel|túnel
crosswalk|senda peatonal
traffic|tráfico, tránsito
traffic light|semáforo
road sign|señal de tránsito, cartel
street light|farol, luminaria
lamp post|farol, poste de luz
parking lot|estacionamiento, playa de estacionamiento
parking|estacionamiento
city|ciudad
urban area|zona urbana, ciudad
metropolis|ciudad, metrópolis
skyscraper|rascacielos
tower|torre
tower block|edificio, torre
building|edificio
house|casa
home|casa, hogar
residential area|barrio, zona residencial
neighbourhood|barrio
cottage|casita, cabaña
cabin|cabaña
castle|castillo
palace|palacio
church|iglesia
cathedral|catedral
temple|templo
monument|monumento
statue|estatua
sculpture|escultura
fountain|fuente
plaza|plaza
square|plaza
park|parque
playground|plaza de juegos, juegos
amusement park|parque de diversiones
stadium|estadio, cancha
sports venue|estadio, cancha
arena|estadio, arena
field|campo, cancha
farm|campo, chacra, finca
agriculture|agricultura, campo
vineyard|viñedo, viña
crop|cultivo
harvest|cosecha
garden hose|manguera
fence|cerco, reja, alambrado
gate|portón, puerta
wall|pared, muro
brick|ladrillo
brickwork|ladrillos, mampostería
concrete|hormigón, cemento
floor|piso
flooring|piso
tile|baldosa, cerámico
ceiling|techo, cielorraso
roof|techo, tejado
window|ventana
door|puerta
doorway|puerta, entrada
stairs|escalera, escaleras
staircase|escalera
balcony|balcón
porch|galería, porche
facade|fachada
architecture|arquitectura
interior design|decoración, diseño de interiores
room|habitación, ambiente
living room|living, sala de estar
bedroom|dormitorio, habitación
bathroom|baño
kitchen|cocina
dining room|comedor
office|oficina
classroom|aula, salón de clases
school|escuela, colegio
hospital|hospital
shop|negocio, local, tienda
store|negocio, local, tienda
shopping|compras
supermarket|supermercado
market|mercado, feria
restaurant|restaurante
cafe|café, bar
bar|bar
hotel|hotel
airport|aeropuerto
station|estación
train station|estación de tren
bus stop|parada de colectivo
furniture|mueble, muebles
table|mesa
desk|escritorio
chair|silla
armchair|sillón
couch|sillón, sofá
sofa|sofá, sillón
bed|cama
mattress|colchón
pillow|almohada
blanket|manta, frazada
curtain|cortina
shelf|estante
shelving|estantería
bookcase|biblioteca, estantería
cabinet|mueble, gabinete
cupboard|alacena, placard
wardrobe|placard, ropero
drawer|cajón
mirror|espejo
lamp|lámpara
lighting|iluminación
light fixture|lámpara, luminaria
chandelier|araña de luces
candle holder|candelabro
carpet|alfombra
rug|alfombra
clock|reloj
vase|jarrón, florero
picture frame|portarretrato, cuadro
painting|pintura, cuadro
art|arte
visual arts|arte, artes visuales
drawing|dibujo
illustration|ilustración
poster|póster, afiche
graffiti|grafiti
mural|mural
photography|fotografía
photograph|fotografía
image|imagen
cartoon|dibujo animado
animation|animación
anime|anime
comics|historieta, cómic
toy|juguete
doll|muñeca
stuffed toy|peluche
teddy bear|osito de peluche, peluche
lego|lego
game|juego
board game|juego de mesa
puzzle|rompecabezas
playing card|carta, naipe
chess|ajedrez
video game|videojuego
console|consola
joystick|joystick
sports equipment|equipo deportivo
sport|deporte
ball|pelota, balón
football|fútbol, pelota de fútbol
soccer|fútbol
basketball|básquet, básquetbol
volleyball|vóley
tennis|tenis
tennis racket|raqueta
golf|golf
rugby|rugby
baseball|béisbol
cricket|cricket
hockey|hockey
skateboard|skate, patineta
skateboarding|skate
surfing|surf
surfboard|tabla de surf
skiing|esquí
ski|esquí
snowboard|snowboard
cycling|ciclismo
bicycle|bicicleta, bici
bicycle wheel|rueda de bicicleta
bicycle frame|cuadro de bicicleta
bike|bici, bicicleta
mountain bike|bicicleta de montaña, bici
running|correr, running
marathon|maratón
swimming|natación, nadar
gym|gimnasio
fitness|entrenamiento, gimnasio
weights|pesas
yoga|yoga
boxing|boxeo
martial arts|artes marciales
horse riding|equitación, cabalgata
fishing|pesca
rowing|remo
sailing|navegación, vela
boat|barco, bote, lancha
ship|barco, buque
yacht|yate
sailboat|velero
canoe|canoa
kayak|kayak
watercraft|embarcación, barco
motorboat|lancha
vehicle|vehículo
car|auto, coche, carro
automotive design|auto, vehículo
automotive exterior|exterior del auto, auto
automotive lighting|faros, luces del auto, auto
automotive tire|neumático, cubierta, rueda
automotive wheel system|rueda, llanta
automotive mirror|espejo retrovisor, auto
automotive parking light|luces de posición, auto
automotive window part|ventanilla, auto
automotive side-view mirror|espejo lateral, auto
hood|capó
bumper|paraguolpes, paragolpes
grille|parrilla, rejilla
headlamp|faro, óptica
tail light|luz trasera, faro trasero
license plate|patente, matrícula
vehicle registration plate|patente, matrícula
number plate|patente
steering wheel|volante
dashboard|tablero, torpedo
car seat|asiento de auto
seat belt|cinturón de seguridad
wheel|rueda
tire|neumático, cubierta
rim|llanta, aro
alloy wheel|llanta de aleación, llanta
spoke|rayo, radio
tread|banda de rodamiento
motor vehicle|vehículo, auto
land vehicle|vehículo, auto
luxury vehicle|auto de lujo, vehículo
sports car|deportivo, auto deportivo
supercar|deportivo, auto deportivo
sedan|sedán, auto
hatchback|hatchback, auto
suv|suv, camioneta
sport utility vehicle|suv, camioneta
crossover suv|suv, camioneta
minivan|minivan, utilitario
van|furgón, utilitario, camioneta
pickup truck|camioneta, pickup
pickup|camioneta, pickup
truck|camión, camioneta
commercial vehicle|vehículo comercial, utilitario
semi-trailer truck|camión, tráiler
trailer|acoplado, tráiler
tow truck|grúa
garbage truck|camión recolector
fire engine|autobomba, bomberos
emergency vehicle|vehículo de emergencia
ambulance|ambulancia
police car|patrullero, móvil policial
taxi|taxi
bus|colectivo, ómnibus, micro
public transport|transporte público
minibus|combi, minibús
tram|tranvía
train|tren
railway|ferrocarril, vías
rail transport|tren, ferrocarril
locomotive|locomotora
subway|subte, metro
motorcycle|moto, motocicleta
motorcycle helmet|casco de moto, casco
motorcycling|motociclismo, moto
scooter|scooter, moto
moped|ciclomotor, moto
motor scooter|scooter, moto
helmet|casco
tractor|tractor
bulldozer|topadora
crane|grúa
construction equipment|maquinaria de obra
excavator|excavadora
forklift|autoelevador
aircraft|avión, aeronave
airplane|avión
aviation|aviación, avión
airliner|avión de pasajeros, avión
helicopter|helicóptero
jet|avión a reacción, jet
drone|dron
rocket|cohete
spacecraft|nave espacial
carriage|carruaje
cart|carro, carrito
wagon|carreta, vagón
rickshaw|rickshaw
wheelchair|silla de ruedas
stroller|cochecito
vehicle door|puerta de auto
fuel|combustible, nafta
gas station|estación de servicio, YPF
engine|motor
auto part|repuesto, autoparte
machine|máquina
motor|motor
tool|herramienta
hand tool|herramienta de mano
hammer|martillo
screwdriver|destornillador
wrench|llave inglesa, herramienta
pliers|pinza, alicate
drill|taladro
saw|sierra
ladder|escalera
hardware|ferretería
rope|soga, cuerda
chain|cadena
key|llave, llaves
keychain|llavero
lock|candado, cerradura
padlock|candado
safe|caja fuerte
bicycle lock|candado de bici
screw|tornillo
nail|clavo
metal|metal
steel|acero
iron|hierro
copper|cobre
wood|madera
hardwood|madera dura, madera
plank|tabla
log|tronco, leña
firewood|leña
plastic|plástico
glass|vidrio
leather|cuero
fabric|tela
textile|tela, textil
wool|lana
denim|jean
paper|papel
paper product|papel
cardboard|cartón
carton|cartón, caja
box|caja
package|paquete, envío
packaging and labeling|embalaje, envase
bottle|botella
water bottle|botella de agua
plastic bottle|botella de plástico, botella
glass bottle|botella de vidrio
jar|frasco
can|lata
tin can|lata
container|envase, recipiente
barrel|barril, tonel
bucket|balde
tank|tanque
pipe|caño, tubo
wire|cable, alambre
cable|cable
electrical wiring|cableado, cables
plug|enchufe
electronics|electrónica, electrónico
electronic device|dispositivo electrónico, aparato
gadget|aparato, dispositivo
technology|tecnología
mobile phone|celular, teléfono, móvil
smartphone|celular, smartphone, teléfono
telephone|teléfono
phone|teléfono, celular
mobile phone case|funda de celular, funda
communication device|dispositivo de comunicación, celular
tablet computer|tablet
laptop|notebook, laptop, computadora
netbook|notebook, laptop
computer|computadora, pc, compu
personal computer|computadora, pc
desktop computer|computadora de escritorio, pc
computer monitor|monitor, pantalla
computer keyboard|teclado
computer mouse|mouse
computer hardware|hardware, computadora
output device|pantalla, monitor
display device|pantalla
screen|pantalla
television|televisor, tele, tv
flat panel display|pantalla plana, tele
projector|proyector
camera|cámara
digital camera|cámara digital, cámara
camera lens|lente de cámara, lente
lens|lente
video camera|filmadora, cámara de video
film|película, film
video|video
cinema|cine
printer|impresora
scanner|escáner
fax|fax
radio|radio
antenna|antena
satellite dish|antena satelital
router|router, módem
battery|batería, pila
charger|cargador
power bank|batería externa
solar panel|panel solar
wind turbine|molino de viento, aerogenerador
windmill|molino
electric pole|poste de luz
transformer|transformador
power line|cable de alta tensión, cables
light bulb|lamparita, foco
led|led
flashlight|linterna
fan|ventilador
air conditioner|aire acondicionado
heater|calefactor, estufa
stove|cocina, estufa
oven|horno
microwave oven|microondas
refrigerator|heladera
washing machine|lavarropas
home appliance|electrodoméstico
kitchen appliance|electrodoméstico, artefacto de cocina
small appliance|electrodoméstico
blender|licuadora
toaster|tostadora
kettle|pava, caldera
coffee maker|cafetera
iron clothes|plancha
vacuum cleaner|aspiradora
sink|pileta, lavatorio
bathtub|bañera, bañadera
toilet|inodoro
shower|ducha
faucet|canilla, grifo
tap|canilla
towel|toalla
soap|jabón
toothbrush|cepillo de dientes
cosmetics|cosmética, maquillaje
makeup|maquillaje
lipstick|labial
perfume|perfume
nail polish|esmalte de uñas, esmalte
manicure|manicura, uñas
nail care|cuidado de uñas, uñas
hair coloring|tintura
hair salon|peluquería
barber|barbero, peluquero
haircut|corte de pelo
spa|spa
massage|masaje
medicine|medicamento, remedio
pill|pastilla, comprimido
syringe|jeringa
stethoscope|estetoscopio
doctor|médico, doctor
nurse|enfermero, enfermera
dentist|dentista, odontólogo
hospital bed|cama de hospital
first aid|primeros auxilios
food|comida
meal|comida, plato
dish|plato
cuisine|cocina, comida
fast food|comida rápida
junk food|comida chatarra
comfort food|comida casera
breakfast|desayuno
brunch|brunch
lunch|almuerzo
dinner|cena
snack|snack, merienda
dessert|postre
sweet|dulce
candy|caramelo, golosina
chocolate|chocolate
ice cream|helado
cookie|galletita
biscuit|galletita
bread|pan
baking|repostería, horneado
pastry|masa, pastelería, facturas
pie|tarta
pizza|pizza
sandwich|sándwich
burger|hamburguesa
hamburger|hamburguesa
hot dog|pancho, hot dog
french fries|papas fritas
fried food|fritura
sushi|sushi
pasta|pasta, fideos
noodle|fideos
rice|arroz
soup|sopa
salad|ensalada
steak|bife, carne
meat|carne
beef|carne vacuna, carne
barbecue|asado, parrilla
grilling|asado, parrilla
roast|asado
sausage|chorizo, salchicha
bacon|panceta, bacon
ham|jamón
cheese|queso
egg|huevo
fish dish|pescado
seafood|mariscos
shrimp|camarón, langostino
crab|cangrejo
lobster|langosta
fruit|fruta, frutas
apple|manzana
banana|banana
orange|naranja
citrus|cítricos
lemon|limón
grape|uva
grapes|uvas
strawberry|frutilla
berry|frutos rojos, baya
watermelon|sandía
melon|melón
peach|durazno
pear|pera
cherry|cereza
pineapple|ananá
mango|mango
avocado|palta
vegetable|verdura, verduras
tomato|tomate
potato|papa
onion|cebolla
garlic|ajo
carrot|zanahoria
pepper|morrón, ají, pimiento
cucumber|pepino
lettuce|lechuga
broccoli|brócoli
corn|choclo, maíz
mushroom|hongo, champiñón
pumpkin|zapallo, calabaza
bean|poroto
nut|nuez, fruto seco
herb|hierba
spice|especia
seasoning|condimento
sauce|salsa
condiment|condimento
honey|miel
jam|mermelada
sugar|azúcar
flour|harina
salt|sal
butter|manteca
milk|leche
dairy|lácteos
yogurt|yogur
drink|bebida
beverage|bebida
non-alcoholic beverage|bebida sin alcohol, gaseosa
soft drink|gaseosa
juice|jugo
coffee|café
espresso|café, espresso
tea|té
mate|mate
cup|taza, vaso
mug|taza, tazón
coffee cup|taza de café, café
teacup|taza de té
glass of water|vaso de agua
drinking glass|vaso
wine|vino
wine glass|copa de vino, copa
red wine|vino tinto
white wine|vino blanco
wine bottle|botella de vino
beer|cerveza
beer glass|vaso de cerveza, cerveza
cocktail|trago, cóctel
liquor|licor, bebida alcohólica
alcoholic beverage|bebida alcohólica, trago
champagne|champán
whisky|whisky
tableware|vajilla
dishware|vajilla
plate|plato
bowl|bowl, cuenco
fork|tenedor
knife|cuchillo
spoon|cuchara
cutlery|cubiertos
chopsticks|palillos
tray|bandeja
napkin|servilleta
tablecloth|mantel
kitchen utensil|utensilio de cocina
cookware and bakeware|ollas, utensilios de cocina
pan|sartén
pot|olla
cooking|cocinar, cocina
chef|cocinero, chef
kitchenware|utensilios de cocina
text|texto, letras, escrito
font|tipografía, letras, texto
number|número, números
handwriting|escritura a mano, letra
writing|escritura, escribir
calligraphy|caligrafía
logo|logo, logotipo
brand|marca
signage|cartel, señalética
sign|cartel, letrero, señal
banner|banner, pancarta
billboard|cartelera, cartel
label|etiqueta
advertising|publicidad
flyer|volante, folleto
brochure|folleto
document|documento
paper document|documento, papel
form|formulario
contract|contrato
invoice|factura
receipt|ticket, recibo
certificate|certificado
diploma|diploma, título
newspaper|diario, periódico
magazine|revista
book|libro
publication|publicación
novel|novela
library|biblioteca
notebook|cuaderno, libreta
notebook paper|cuaderno, hoja
pen|lapicera, birome, boli
pencil|lápiz
marker|marcador, fibrón
crayon|crayón
eraser|goma de borrar
stationery|papelería, librería
office supplies|útiles de oficina
folder|carpeta
file folder|carpeta
binder|carpeta
envelope|sobre
mail|correo
postage stamp|estampilla, sello
stamp|sello
map|mapa
globe|globo terráqueo
calendar|calendario
chart|gráfico
diagram|diagrama, esquema
graph|gráfico
table of data|tabla, planilla
spreadsheet|planilla de cálculo, planilla
presentation|presentación
whiteboard|pizarra
blackboard|pizarrón
screenshot|captura de pantalla, captura
website|sitio web, web
web page|página web
software|software, programa
multimedia|multimedia
qr code|código qr, qr
barcode|código de barras
money|dinero, plata
cash|efectivo, plata
currency|moneda, dinero
coin|moneda
banknote|billete
credit card|tarjeta de crédito, tarjeta
card|tarjeta
business card|tarjeta personal
identity document|documento de identidad, dni
passport|pasaporte
ticket|entrada, boleto, ticket
coupon|cupón
sale|oferta, venta
price tag|etiqueta de precio, precio
cart shopping|carrito de compras
shopping bag|bolsa de compras, bolsa
vendor|vendedor
market stall|puesto, feria
person holding|persona sosteniendo
event ticket|entrada
hobby|pasatiempo
craft|manualidad
knitting|tejido, tejer
sewing|costura
embroidery|bordado
pottery|cerámica, alfarería
ceramic|cerámica
porcelain|porcelana
crystal|cristal
gem|gema, piedra preciosa
diamond|diamante
gold|oro
silver|plata
trophy|trofeo, copa
medal|medalla
award|premio
flag|bandera
banner flag|bandera
national flag|bandera nacional
mountain range|cordillera
glacier|glaciar
canyon|cañón
cave|cueva
geology|geología
terrain|terreno
plain|llanura, pampa
wilderness|zona silvestre
meadow|prado
pasture|pastura, potrero
savanna|sabana
tundra|tundra
reef|arrecife
coral|coral
underwater|bajo el agua
aquarium|acuario
zoo|zoológico
safari|safari
wildlife|fauna silvestre, animales
fauna|fauna
vertebrate|vertebrado
organism|organismo
macro photography|macro, fotografía macro
close-up|primer plano
sky and clouds|cielo, nubes
daytime|día
evening|tarde, atardecer
atmosphere|atmósfera, cielo
atmospheric phenomenon|fenómeno atmosférico
cumulus|nube cúmulo, nubes
space|espacio
astronomy|astronomía
galaxy|galaxia
planet|planeta
earth|tierra
horizon over water|horizonte
tourism|turismo
tourist attraction|atracción turística, turismo
landmark|lugar emblemático, monumento
travel|viaje
vacation|vacaciones
holiday|feriado, vacaciones
resort|complejo turístico, resort
recreation|recreación
leisure|ocio, tiempo libre
picnic|picnic
tourist|turista
adventure|aventura
sports uniform|camiseta deportiva, uniforme
sports jersey|camiseta
jersey|camiseta
player|jugador
athlete|deportista, atleta
competition|competencia
race|carrera
racing|carrera, competencia
motorsport|automovilismo
rallying|rally
formula one|fórmula 1
race track|autódromo, pista
audience|público
spectator|espectador
crowd cheering|hinchada, público
protest|protesta, marcha
parade|desfile
procession|procesión
ceremony|ceremonia
graduation|graduación, egreso
meeting|reunión
conference|conferencia
seminar|seminario
training|capacitación
lecture|clase, charla
presentation slide|diapositiva
teacher|docente, maestro, profesor
student|estudiante, alumno
class|clase
exam|examen
child art|dibujo infantil
soldier|soldado
military|militar
army|ejército
police|policía
firefighter|bombero
engineer|ingeniero
worker|trabajador, obrero
construction|obra, construcción
construction worker|obrero, albañil
architecture site|obra
scaffolding|andamio
factory|fábrica
industry|industria
warehouse|depósito, galpón
shipping|envíos, logística
cargo|carga
container ship|buque de carga, barco
port|puerto
harbor|puerto
dock|muelle
pier|muelle
lighthouse|faro
anchor|ancla
dummy|maniquí
mannequin|maniquí
showroom|salón de exhibición, concesionaria
dealership|concesionaria, agencia
auto show|exposición de autos, muestra
motor show|exposición de autos
exhibition|exposición, muestra
museum|museo
gallery|galería
theatre|teatro
theater|teatro
opera|ópera
circus|circo
carnival|carnaval
festival|festival
fair|feria
`;
const ES2EN = {}, EN2ES = {};
const quitar = s => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').trim();
L.split('\n').forEach(l => {
  const p = l.split('|'); if (p.length < 2) return;
  const en = p[0].trim().toLowerCase();
  const es = p[1].split(',').map(x => x.trim()).filter(Boolean);
  if (!en || !es.length) return;
  EN2ES[en] = es[0];
  es.forEach(e => { const k = quitar(e); (ES2EN[k] = ES2EN[k] || new Set()).add(en); });
});
/* singular/plural simple para castellano */
function formas(k){
  const f = new Set([k]);
  if (k.length > 3){
    if (k.endsWith('es')) f.add(k.slice(0, -2));
    if (k.endsWith('s')) f.add(k.slice(0, -1));
    if (k.endsWith('ces')) f.add(k.slice(0, -3) + 'z');
    f.add(k + 's'); f.add(k + 'es');
  }
  return [...f];
}
window.ETQ = {
  /* palabra escrita en castellano → etiquetas en inglés (vacío si no está en el diccionario) */
  aEtiquetas(palabra){
    const k = quitar(palabra), out = new Set();
    formas(k).forEach(x => { if (ES2EN[x]) ES2EN[x].forEach(v => out.add(v)); });
    /* también lo que ya está escrito en inglés */
    if (EN2ES[k]) out.add(k);
    return [...out];
  },
  /* "car,vehicle" → "auto, vehículo" */
  mostrar(csv){
    if (!csv) return '';
    const vistos = new Set(), out = [];
    csv.split(',').forEach(e => { e = e.trim(); const t = EN2ES[e] || e; if (!vistos.has(t)){ vistos.add(t); out.push(t); } });
    return out.join(', ');
  },
  cantidad: Object.keys(EN2ES).length
};
})();
