import sys
import random
import math

def ability_mod(n):
	return math.floor((n-10)/2)
	
def scores_to_mod(a):
	return [ability_mod(x) for x in a]

def ability(keep=3, roll=4):
	return [sum(sorted([random.randint(1,6) for x in range(roll)])[roll-keep:]) for _ in range(6)]

def roll(num, type, mod = 0):
	return sum([random.randint(1,type) for _ in range(num)])+mod

def sroll(desc):
	if "+" in desc:
		sp = desc.split("+")
		sp2 = sp[0].split("d")
		mod = int(sp[1])
		type = int(sp2[1])
		num = int(sp2[0])
	elif "-" in desc:
		sp = desc.split("-")
		sp2 = sp[0].split("d")
		mod = int(sp[1])*-1
		type = int(sp2[1])
		num = int(sp2[0])
	else:
		sp2 = desc.split("d")
		type = int(sp2[1])
		num = int(sp2[0])
		mod = 0
	return roll(num, type,mod)

def main(argc = len(sys.argv), args = sys.argv):
	return

if __name__ == "__main__":
	main()