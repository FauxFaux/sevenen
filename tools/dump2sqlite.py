import json
import sqlite3

con = sqlite3.connect('sdr.sqlite3')
cur = con.cursor()

j = json.load(open('db.json'))

for name, data in j.items():
    cur.execute('create table {} ({})'.format(name, ','.join(data['cols'])))
    for row in data['rows']:
        cur.execute('insert into {} values ({})'.format(name, ','.join('?' for _ in row)), row)

con.commit()
