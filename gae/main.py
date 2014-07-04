#!/usr/bin/env python
#
# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#	 http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import webapp2
import json
from randomdotorg import *
import dnd
import datetime

from google.appengine.ext import ndb
from google.appengine.api import users
from google.appengine.api import taskqueue

class Player(ndb.Model):
	name = ndb.StringProperty(indexed=True,required=True)
	imei = ndb.StringProperty(indexed=True,required=True)
	group = ndb.StringProperty(indexed=True)
	rolling_history = ndb.JsonProperty(repeated=True)
	date = ndb.DateTimeProperty(auto_now_add=True)

class Group(ndb.Model):
	name = ndb.StringProperty(indexed=True,required=True)
	members = ndb.StringProperty(repeated=True)
	date = ndb.DateTimeProperty(auto_now_add=True)


class MainHandle(webapp2.RequestHandler):
	# def post(self):
		# user = users.get_current_user()
		# if user:
			# greeting = ('Welcome, %s! (<a href="%s">sign out</a>)' %
						# (user.nickname(), users.create_logout_url('/')))
		# else:
			# greeting = ('<a href="%s">Sign in or register</a>.' %
						# users.create_login_url('/'))
		# self.response.write(greeting)
	def post(self):
		info = {'derp':[5,6]}
		q = self.request.get('quota')
		if q != '':
			info = RandomDotOrg('party_dice_gae').get_quota()
		self.response.write(json.dumps(info))
	def get(self):
		self.post()

class RollHandle(webapp2.RequestHandler):
	def post(self):
		n = self.request.get('n')
		t = self.request.get('t')
		m = self.request.get('m')
		imei = self.request.get('i')
		if (not n.isdigit()) or\
			(not t.isdigit()) or\
			(not (m.isdigit() or m[1:].isdigit())) or\
			imei == '':
			r = [n,t,m,imei]
		else:
			r = dnd.roll(int(n), int(t), int(m))
			self.response.write(json.dumps(r))
			qry = Player.query(Player.imei == imei)
			if qry.count() > 0:
				p = qry.fetch()[0]
				p.rolling_history.append(json.dumps((r,"{0}d{1}+{2}".format(n,t,m),datetime.datetime.today().strftime("%a %d %I:%M %p"))))
				p.rolling_history = p.rolling_history[-5:]
				p.put()
	def get(self):
		self.post()
		
class UserHandle(webapp2.RequestHandler):
	# @ndb.transactional(retries=3)
	def user_check(self, name, group, imei):
		qry = Player.query(Player.imei == imei)
		if qry.count() > 0:
			p = qry.fetch()[0]
			if name != '':
				p.name = name
			if group != '':
				taskqueue.add(url='/groupw', params={'o': p.group, 'n': group, 'i': imei})
				p.group = group
			p.put()
		else:
			p = Player(name = name, imei = imei, rolling_history = [])
			p.put()
		return json.dumps({'n':p.name, 'i':p.imei, 'h':p.rolling_history, 'd':str(p.date), 'g':p.group})
	def post(self):
		n = self.request.get('n')
		g = self.request.get('g')
		i = self.request.get('i')
		if i != '':
			self.response.write(self.user_check(n,g,i))
		else:
			self.response.write(json.dumps((n,g,i,self.request.url,self.request.body)))
	def get(self):
		self.post()

class GroupWorker(webapp2.RequestHandler):
	def post(self): # should run at most 1/s due to entity group limit
		o = self.request.get('o')
		n = self.request.get('n')
		i = self.request.get('i')
		@ndb.transactional(retries=3, xg=True)
		def update_group():
			
			g = Group.get_or_insert(o, name = o, members = [])
			g.members = [x for x in g.members if x != i]
			if len(g.members) > 0:
				g.put()
			else:
				g.key.delete()
			
			g = Group.get_or_insert(n, name = n, members = [])
			if i not in g.members:
				g.members.append(i)
			g.put()
		update_group()

class GroupHandle(webapp2.RequestHandler):
	def post(self):
		r = []
		n = self.request.get('n')
		if n != '':
			qry = Group.query(Group.name == n)
			if qry.count() > 0:
				g = qry.fetch(1)[0]
				ms = g.members
				for m in ms:
					qry = Player.query(Player.imei == m)
					p = qry.fetch()[0]
					r += [(p.name,p.rolling_history)]
		self.response.write(json.dumps(r))
	def get(self):
		self.post()

app = webapp2.WSGIApplication([
	('/', MainHandle),
	('/roll', RollHandle),
	('/group', GroupHandle),
	('/groupw', GroupWorker),
	('/user', UserHandle)
], debug=True)
