INSERT INTO manufacturer (name) VALUES ('Bosch'), ('Samsung'), ('LG'), ('Philips'), ('Dyson'), ('Xiaomi'), ('Siemens');

INSERT INTO appliance (name, category, model, manufacturer_id, power_type, characteristic, description, power, price) VALUES
  ('Washing machine',  'BIG',   'Series 6 WGG244A0',  1, 'AC220',       '9 kg, 1400 rpm',        'Front-loading washing machine',        2300,  649.00),
  ('Refrigerator',     'BIG',   'RB34C670',           2, 'AC220',       '344 l, No Frost',       'Fridge-freezer with inverter compressor', 180, 799.00),
  ('Dishwasher',       'BIG',   'DF365101',           7, 'AC220',       '13 place settings',     'Built-in dishwasher',                  1900,  529.00),
  ('Vacuum cleaner',   'SMALL', 'V15 Detect',         5, 'ACCUMULATOR', '60 min runtime',        'Cordless vacuum cleaner',               660,  699.00),
  ('Hair dryer',       'SMALL', 'HP8230',             4, 'AC220',       'Ionic, 2200 W',         'Hair dryer with ionic care',           2200,   39.90),
  ('Robot vacuum',     'SMALL', 'Robot Vacuum S10',   6, 'ACCUMULATOR', '4000 Pa suction',       'Robot vacuum cleaner with mop',          70,  249.00),
  ('Microwave oven',   'SMALL', 'MS23K3513AK',        2, 'AC110',       '23 l, 800 W',           'Solo microwave oven',                   800,   99.00);
