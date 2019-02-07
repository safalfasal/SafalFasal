import datetime
import torch
import os
from flask import Flask , render_template , url_for , request
import base64
from flask_mysqldb import MySQL
#import cv2
from fastai.imports import *

from fastai.transforms import *
from fastai.conv_learner import *
from fastai.model import *
from fastai.dataset import *
from fastai.sgdr import *
from fastai.plots import *
    # resnext101_64
PATH = "Path to your data set directory including a folder named 'models' containing weights"
sz=299
arch=resnext50
bs=14

app = Flask(__name__)

app.config['MYSQL_HOST'] = 'localhost'
app.config['MYSQL_USER'] = 'root'
app.config['MYSQL_PASSWORD'] = '1234'
app.config['MYSQL_DB'] = 'HELLO'

mysql = MySQL(app)

APP_ROOT = os.path.dirname(os.path.abspath(__file__))




tfms = tfms_from_model(arch, sz, aug_tfms=transforms_side_on, max_zoom=1.1)
data = ImageClassifierData.from_paths(PATH, tfms=tfms, bs=bs, num_workers=4)
learn = ConvLearner.pretrained(arch, data, precompute=False)

learn.load('224_all_50')

def pred(filepath):
	print('Hello')	
	trn_tfms, val_tfms = tfms_from_model(arch,100)
	img = open_image(filepath)
	im = val_tfms(img)
	preds = learn.predict_array(im[None])
	index = np.argmax(preds)
	print(data.classes[index])	
	print('Hello')
	return str(data.classes[index])




@app.route("/")
def index():
   return render_template("user.html")

@app.route('/info')
def info():
    cur = mysql.connection.cursor()
    
    resultValue = cur.execute("SELECT * FROM crop")
    if resultValue >= 0:
        user_det = cur.fetchall()
        return render_template('user.html' , user_det = user_det)
    
     
@app.route("/upload" , methods = ['POST'])
def upload():
       uploaded_files = request.files.getlist("file")
       print(len(uploaded_files))
       target = os.path.join(APP_ROOT , 'images/')
       print(target)
       
       if not os.path.isdir(target):
          os.mkdir(target)
      

       for file in uploaded_files:
         print(file)
         print("hello")
         filename = file.filename
         destination = "".join([target , filename])
         print(destination)
         file.save(destination)
       return render_template("complete.html")

@app.route("/processjson" , methods = ['POST'])
def processjson():
     
     
     data = request.get_json()
     return "{'response':'Yes'}"
     #print(request.is_json)
     imgdata = base64.b64decode(data['image'])
     now  = datetime.datetime.now()
     date_current = str(now)
     image_name = 'img'+ date_current + '.jpg'
     filename = image_name
     if not os.path.exists('INPUT_IMAGES'):
           os.makedirs('INPUT_IMAGES')
      
     folder_name = str('INPUT_IMAGES/')
     imagepath = os.path.join(folder_name,filename) 
     with open(os.path.join(folder_name,filename), 'wb') as f:
        f.write(imgdata)
     #print(data['image'])
     
     region = data['name']
     if region == "":
         region = 'Region Not specified'
     print("hello")
     cur = mysql.connection.cursor()
     cur.execute("INSERT INTO crop (TIMESTAMP ,FILENAME , REGION) VALUES(%s ,%s , %s)",(date_current,image_name , region))
     mysql.connection.commit()
     cur.close()
     response = pred(imagepath)
     
     return "{'response':'"+response+"'}" 

if __name__ == "__main__":
   app.run(debug = False , host = '0.0.0.0')
