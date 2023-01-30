# ASSIGNMENT 3 ADDITION - Created by Huntermuze
STANDARD_PREFIX = "CRYPTLOG [CLIENT SIDE] >> "

# Extremely simple logging system that prepends a constant prefix before log entries.
def log(message):
    print(STANDARD_PREFIX + str(message))