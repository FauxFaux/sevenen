import datetime
import decimal
import json
import sys

import fdb


def js(val):
    if type(val) == int:
        return val
    if type(val) == str:
        return val
    if val is None:
        return val
    if type(val) == decimal.Decimal:
        return str(val)
    if type(val) == datetime.datetime:
        return val.isoformat()
    raise Exception(type(val))


con = fdb.connect(dsn='SDR.FDB', user='sysdba', password='masterkey')

cur = con.cursor()
cur.execute("SELECT a.RDB$RELATION_NAME FROM RDB$RELATIONS a WHERE RDB$SYSTEM_FLAG=0")

tables = [row[0].strip() for row in cur.fetchall()]

db = {}

for table in tables:
    db[table] = {}
    cur.execute(
        f"select rdb$field_name from rdb$relation_fields where rdb$relation_name='{table}' order by rdb$field_position")

    db[table]['cols'] = [head[0].strip() for head in cur.fetchall()]

    cur.execute(f"select * from {table}")

    db[table]['rows'] = [[js(field) for field in row] for row in cur.fetchall()]

json.dump(db, sys.stdout)
