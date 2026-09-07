"""Authored conceptual body plans. Dimensions are drawing proportions, never measurements."""
PROFILES={}
def group(names,base,**kwargs):
 for n in names.split():PROFILES[n.lower()]=dict(base=base,**kwargs)
group('Quetzalcoatlus Hatzegopteryx','pteranodon',neck=2.7,neckRise=.2,head=.95,crest='none')
group('Pterodactylus','pteranodon',neck=1.6,head=.65,crest='none',teeth=True)
group('Tropeognathus','pteranodon',neck=1.15,head=1.2,crest='keel',teeth=True)
group('Rhamphorhynchus','pteranodon',neck=.8,head=.55,tail=5.5,crest='none',teeth=True)
group('Dimorphodon','pteranodon',neck=.8,head=.48,headHeight=2.2,tail=5,crest='none',teeth=True)
group('Dilophosaurus','allosaurus',head=.85,crest='double',arm=1.05)
group('Cryolophosaurus','allosaurus',head=.86,crest='fan',arm=1.1)
group('Therizinosaurus','allosaurus',neck=1.65,head=.48,headHeight=.65,body=1.35,arm=2.8,claw=3,tail=.65,teeth=False)
group('Deinocheirus','allosaurus',neck=1.5,head=1.1,headHeight=.6,body=1.35,arm=2.6,tail=.8,sail=.7,teeth=False)
group('Gallimimus','allosaurus',neck=1.55,head=.52,headHeight=.5,body=.8,arm=1.4,teeth=False)
group('Deinonychus','velociraptor',head=1.12,headHeight=1.15,arm=1.12)
group('Utahraptor','velociraptor',head=1.22,headHeight=1.3,body=1.15,arm=1.05)
group('Pyroraptor','velociraptor',head=.9,neck=.9,arm=1.15)
group('Microraptor','archaeopteryx',head=.86,tail=1.2,arm=1.1)
group('Anchiornis','archaeopteryx',head=.85,tail=.8,arm=.9)
group('Troodon','velociraptor',head=.8,headHeight=1.15,neck=1.15,arm=.9)
group('Carnotaurus','allosaurus',head=.7,headHeight=1.3,arm=.35,crest='horns')
group('Majungasaurus Rajasaurus','allosaurus',head=.75,headHeight=1.3,arm=.4,crest='nasal')
group('Ceratosaurus','allosaurus',head=.88,headHeight=1.25,arm=.7,crest='nasal')
group('Pachycephalosaurus','parasaurolophus',head=.55,headHeight=.8,body=.85,crest='dome',quadruped=False)
group('Brontosaurus Apatosaurus','brachiosaurus',neck=1.7,neckRise=.25,tail=1.65,head=.9,shoulder=-.75)
group('Diplodocus','brachiosaurus',neck=2,neckRise=.18,tail=2.5,head=.8,shoulder=-.8)
group('Argentinosaurus Patagotitan Dreadnoughtus Alamosaurus','brachiosaurus',neck=1.65,neckRise=.5,tail=1.4,head=.85,shoulder=-.55)
group('Titanosaurus Bruhathkayosaurus','brachiosaurus',neck=1.4,neckRise=.5,tail=1.35,shoulder=-.55)
group('Nigersaurus','brachiosaurus',neck=1.1,neckRise=.08,tail=1.3,head=.95,headWidth=1.8,shoulder=-.9)
group('Camarasaurus','brachiosaurus',neck=1.1,neckRise=.6,tail=1.25,head=1.35,headHeight=1.5,shoulder=-.65)
group('Amargasaurus','brachiosaurus',neck=1.4,neckRise=.35,tail=1.3,shoulder=-.7,crest='neckspines')
group('Sauroposeidon','brachiosaurus',neck=1.8,neckRise=1.2,tail=1.2,head=.9)
group('Mamenchisaurus','brachiosaurus',neck=3,neckRise=.35,tail=1.65,shoulder=-.7)
group('Plateosaurus','brachiosaurus',neck=.9,neckRise=.5,tail=1.7,head=1.4,shoulder=.1,quadruped=False,arm=1.4)
group('Iguanodon','parasaurolophus',head=.75,body=1.1,crest='thumb')
group('Edmontosaurus','parasaurolophus',head=1.15,headHeight=.72,crest='none')
group('Maiasaura Hadrosaurus','parasaurolophus',head=.92,headHeight=.85,crest='none')
group('Saurolophus','parasaurolophus',head=.95,crest='spike')
group('Suchomimus Baryonyx Cristatusaurus Irritator','spinosaurus',sail=.05,tail=.88,neck=1.05)
group('Carcharodontosaurus Mapusaurus','giganotosaurus',head=1.05,headHeight=.93,body=1.05)
group('Megalosaurus Torvosaurus','allosaurus',head=1.1,body=1.1,arm=.9,crest='none')
group('Acrocanthosaurus','giganotosaurus',head=1,body=1.05,sail=.7)
group('Concavenator','allosaurus',head=.8,body=.9,sail=1.1,crest='hump')
group('Borealopelta','ankylosaurus',club=False,body=.95,head=.85,crest='shoulderspikes')
group('Herrerasaurus Coelophysis Compsognathus Scipionyx Eoraptor','allosaurus',head=.65,headHeight=.65,neck=1.15,body=.8,arm=1.1,crest='none')
group('Sinosauropteryx','allosaurus',head=.65,headHeight=.6,tail=1.3,body=.75,arm=.75,crest='none')
group('Yutyrannus','tyrannosaurus',head=.95,neck=1.1,arm=1.8,fingers=3)
group('Nanotyrannus','tyrannosaurus',head=.82,headHeight=.8,body=.85,arm=1.8,fingers=2)
group('Tarbosaurus','tyrannosaurus',head=1.05,headWidth=.85,arm=.9)
group('Albertosaurus Gorgosaurus','tyrannosaurus',head=.88,headHeight=.9,neck=1.05,body=.9,arm=1.1)
group('Daspletosaurus Nanuqsaurus','tyrannosaurus',head=.95,headHeight=1.05,body=.95)
group('Dryptosaurus','tyrannosaurus',head=.85,headHeight=.85,arm=2,fingers=3)
group('Australovenator Maip','allosaurus',head=.86,headHeight=.8,arm=1.7,claw=1.7,crest='none')
group('Oviraptor','allosaurus',head=.4,headHeight=.8,neck=1.3,body=.85,arm=1.5,crest='casque',teeth=False)
group('Styracosaurus','triceratops',brow=0,nose=2,frillSpikes=6,frill=1.1,fenestrae=True)
group('Protoceratops','triceratops',brow=0,nose=0,head=.8,frill=.8,fenestrae=True)
group('Psittacosaurus Aquilops','triceratops',brow=0,nose=0,head=.65,frill=.2,quadruped=False,arm=1.2,tail=1.15)
group('Pachyrhinosaurus','triceratops',brow=0,nose=0,crest='boss',frillSpikes=2,fenestrae=True)
group('Torosaurus','triceratops',brow=.8,nose=.7,frill=1.3,fenestrae=True)
group('Tylosaurus','mosasaurus',head=1.1,headHeight=.8,tail=1.15)
group('Plesiosaurus','mosasaurus',neck=5,neckRise=.5,head=.48,tail=.4,body=1.3,flipper=1.5)
group('Elasmosaurus','mosasaurus',neck=11,neckRise=.3,head=.4,tail=.35,body=1.3,flipper=1.55)
group('Liopleurodon Kronosaurus Pliosaurus','mosasaurus',neck=1.2,head=1.65,headHeight=1.5,tail=.4,body=1.5,flipper=1.7)
group('Ichthyosaurus','mosasaurus',neck=.35,head=1.45,headHeight=.7,tail=.6,body=1.1,flipper=.8,crest='fish')
group('Tanystropheus','mosasaurus',neck=9,neckRise=.2,head=.55,tail=.65,body=.7,flipper=.5,crest='coastal')
group('Archelon','mosasaurus',neck=1.2,head=.65,headHeight=1.2,tail=.18,body=1.9,flipper=1.7,crest='shell',teeth=False)
group('Kentrosaurus','stegosaurus',head=.9,neck=1.05,crest='backspikes')
# Saurophaganax's original material is chimeric; show only a clearly labelled sauropod hypothesis.
group('Saurophaganax','brachiosaurus',neck=1.7,neckRise=.2,tail=2.1,head=.9,shoulder=-.8)
# Small authored adjustments distinguish relatives without presenting measurements.
PROFILES['brontosaurus']['body']=1.08
PROFILES['patagotitan']['neck']=1.85
PROFILES['dreadnoughtus']['neck']=1.55
PROFILES['alamosaurus']['neck']=1.45
PROFILES['bruhathkayosaurus']['body']=1.15
PROFILES['maiasaura']['head']=.88
PROFILES['baryonyx']['arm']=1.2
PROFILES['cristatusaurus']['neck']=.9
PROFILES['irritator']['head']=.9
PROFILES['mapusaurus']['head']=.98
PROFILES['torvosaurus']['head']=1.16
PROFILES['rajasaurus']['head']=.8
PROFILES['gorgosaurus']['head']=.95
PROFILES['nanuqsaurus']['head']=.9
PROFILES['maip']['body']=1.2
PROFILES['aquilops']['head']=.58
PROFILES['kronosaurus']['head']=1.9
PROFILES['pliosaurus']['head']=1.8
for name,value in [('coelophysis',.58),('compsognathus',.55),('scipionyx',.62),('eoraptor',.5)]:PROFILES[name]['head']=value

PROFILES['hatzegopteryx'].update(neck=1.7,headWidth=1.5,headHeight=1.3,body=1.2)
