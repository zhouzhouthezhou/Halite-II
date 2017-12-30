import pandas as pd

test = {'a': [4, 2, 4, 6, 4, 6], 'b': [1, 2, 3, 4, 5, 6]}
df = pd.DataFrame(test)
df.set_index('a', inplace=True)
x = 4
try:
    print(df.get('b')[x].head(1)[x])
except:
    print(df.get('b')[x])
