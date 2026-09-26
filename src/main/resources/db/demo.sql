-- EXPLICIT demo installation only. No production accounts/passwords here.
INSERT INTO species(name) VALUES ('Dog'),('Cat') ON CONFLICT DO NOTHING;
INSERT INTO breeds(species_id,name) SELECT id,'Labrador Retriever' FROM species WHERE name='Dog' ON CONFLICT DO NOTHING;
INSERT INTO breeds(species_id,name) SELECT id,'Dachshund' FROM species WHERE name='Dog' ON CONFLICT DO NOTHING;
INSERT INTO breeds(species_id,name) SELECT id,'Rottweiler' FROM species WHERE name='Dog' ON CONFLICT DO NOTHING;
INSERT INTO breeds(species_id,name) SELECT id,'Shih Tzu' FROM species WHERE name='Dog' ON CONFLICT DO NOTHING;
INSERT INTO breeds(species_id,name) SELECT id,'Persian' FROM species WHERE name='Cat' ON CONFLICT DO NOTHING;
INSERT INTO breeds(species_id,name) SELECT id,'American Shorthair' FROM species WHERE name='Cat' ON CONFLICT DO NOTHING;
INSERT INTO breeds(species_id,name) SELECT id,'British Shorthair' FROM species WHERE name='Cat' ON CONFLICT DO NOTHING;
INSERT INTO pets(name,breed_id,birth_date,sex,description,image_path,fee)
 SELECT 'Milo',id,CURRENT_DATE - INTERVAL '18 months','Male','A cheerful walking companion who loves company. Meet Milo at the shelter to learn about his care needs.','/images/labrador.jpg',5000 FROM breeds WHERE name='Labrador Retriever';
INSERT INTO pets(name,breed_id,birth_date,sex,description,image_path,fee)
 SELECT 'Luna',id,CURRENT_DATE - INTERVAL '2 years','Female','A gentle cat looking for a quiet home and a sunny window. Our team can help you plan a calm introduction.','/images/persian.jpg',3500 FROM breeds WHERE name='Persian';
INSERT INTO pets(name,breed_id,birth_date,sex,description,image_path,fee)
 SELECT 'Archie',id,CURRENT_DATE - INTERVAL '10 months','Male','Curious, affectionate and ready to explore. Ask the team about his daily routine and training.','/images/dachshund.jpg',4000 FROM breeds WHERE name='Dachshund';
INSERT INTO pets(name,breed_id,birth_date,sex,description,image_path,fee)
 SELECT 'Cleo',id,CURRENT_DATE - INTERVAL '3 years','Female','An easygoing companion who enjoys gentle play and a comfortable place to rest.','/images/british-shorthair.jpg',3000 FROM breeds WHERE name='British Shorthair';
INSERT INTO inventory_categories(name) VALUES ('Food'),('Care'),('Equipment') ON CONFLICT DO NOTHING;
INSERT INTO inventory(name,category_id,unit,quantity,reorder_level) SELECT 'Adult dog food',id,'kg',0,10 FROM inventory_categories WHERE name='Food';
INSERT INTO inventory(name,category_id,unit,quantity,reorder_level) SELECT 'Cat litter',id,'bags',0,5 FROM inventory_categories WHERE name='Care';
