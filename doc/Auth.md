Authentication
==============

Dictionaries
------------

Dictionaries are stored under `data/dicts`. The current ones are:

 - comprehensive: The YAWL word list
 - normal: `english-words.20`, from the SCOWL word lists, with words
   containing `'s' removed to prevent confusion.
 - basic: Ogden''s Basic English word list.

To add a dictionary, simply place a plain text file with one word per
line in the dictionary directory, and add the appropriate entry for the
dictionary in `SymbolGenerators.java`. The index file will be generated
automatically by `ant`.

All dictionaries are under the public domain.
