import concurrent.futures
import time
import threading
import socket
import matplotlib.pyplot as plt
from matplotlib.pyplot import figure
import math
import numpy as np
import collections
from utils import convert_array_to_signed_int, convert_to_unsigned_long, conv_ecg, conv_imu
from threading import Thread
import pandas as pd

# Animation
from pyqtgraph.Qt import QtGui, QtCore
import numpy as np
import pyqtgraph as pg
from pyqtgraph.ptime import time

global ecg_window, axl_fs_res, gyr_fs_res
ecg_window = 500
axl_fs_res = 0.732
gyr_fs_res = 0.0082

class AnimationWidget(pg.LayoutWidget):

    def __init__(self):
        super(AnimationWidget, self).__init__()
        
        # Less cool, more readable
        #pg.setConfigOption('background', 'w')
        #pg.setConfigOption('foreground', 'k')
        
        self.signalPlot_Top = pg.PlotWidget()
        self.signalPlot_Top.setLabel('bottom', 'ECG')  # , units='TO SET')
        self.signalCurve_Top = pg.PlotCurveItem()
        self.signalPlot_Top.addItem(self.signalCurve_Top)
        self.signalPlot_Top.setRange(QtCore.QRectF(0, -800, ecg_window, 2300))

        self.signalPlot_Mid = pg.PlotWidget()
        self.signalPlot_Mid.setLabel('bottom', 'PPG')  # , units='TO SET')
        self.signalCurve_Mid = pg.PlotCurveItem()
        self.signalPlot_Mid.addItem(self.signalCurve_Mid)
        self.signalPlot_Mid.setRange(QtCore.QRectF(0, 0, 150, 100))

        self.signalPlot_Mid2 = pg.PlotWidget()
        self.signalPlot_Mid2.setLabel('bottom', 'IMU - Gyro ', units='dps')
        self.signalCurve_Gyrx = pg.PlotCurveItem()
        self.signalCurve_Gyry = pg.PlotCurveItem()
        self.signalCurve_Gyrz = pg.PlotCurveItem()
        self.signalPlot_Mid2.addItem(self.signalCurve_Gyrx)
        self.signalPlot_Mid2.addItem(self.signalCurve_Gyry)
        self.signalPlot_Mid2.addItem(self.signalCurve_Gyrz)
        self.signalPlot_Mid2.setRange(QtCore.QRectF(0, -200, 150, 400))


        self.signalPlot_Bot = pg.PlotWidget()
        self.signalPlot_Bot.setLabel('bottom', 'IMU - Acc ', units='mg')
        self.signalCurve_Accx = pg.PlotCurveItem()
        self.signalCurve_Accy = pg.PlotCurveItem()
        self.signalCurve_Accz = pg.PlotCurveItem()
        self.signalPlot_Bot.addItem(self.signalCurve_Accx)
        self.signalPlot_Bot.addItem(self.signalCurve_Accy)
        self.signalPlot_Bot.addItem(self.signalCurve_Accz)
        self.signalPlot_Bot.setRange(QtCore.QRectF(0, -5000, 150, 10000))

        self.layout.addWidget(self.signalPlot_Top)
        self.layout.addWidget(self.signalPlot_Mid)
        self.layout.addWidget(self.signalPlot_Mid2)
        self.layout.addWidget(self.signalPlot_Bot)

    def update(self):
        self.signalCurve_Top.setData(np.array(ecg), pen = 'w')
        self.signalCurve_Mid.setData(np.array(ppg), pen = 'w')
        self.signalCurve_Accx.setData(np.array(imu["acc_x"])*axl_fs_res, pen = 'r')
        self.signalCurve_Accy.setData(np.array(imu["acc_y"])*axl_fs_res, pen = 'g')
        self.signalCurve_Accz.setData(np.array(imu["acc_z"])*axl_fs_res, pen = 'b')
        self.signalCurve_Gyrx.setData(np.array(imu["gyr_x"])*gyr_fs_res, pen = 'r')
        self.signalCurve_Gyry.setData(np.array(imu["gyr_y"])*gyr_fs_res, pen = 'g')
        self.signalCurve_Gyrz.setData(np.array(imu["gyr_z"])*gyr_fs_res, pen = 'b')


def threadEcg():
    global ecg
    ecg_full = []
    ecg = []

    print("Thread Ecg started")

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    # IP and port for accepting connections
    server_address = ('0.0.0.0', 6660)
    # print server address and port
    print("[+] Server IP {} | Port {}".format(server_address[0], server_address[1]))
    # bind socket with server
    sock.bind(server_address)
    # Listen for incoming connections
    sock.listen(10)
    # Create Loop
    while True:
        # Wait for a connection
        # print('[+]  Waiting for a client connection')

        # connection established
        connection, client_address = sock.accept()
        # print('[+] Connection from', client_address)

        received_data = connection.recv(1024)
        trimmed_data = received_data[2:]
        array_data = bytearray()
        vec = np.arange(0, len(trimmed_data), 2)

        for index in vec:
            tmp = trimmed_data[index:index + 2]
            tmp2 = int(tmp, 16)
            array_data.append(int(tmp2))
        ecg_track = conv_ecg(array_data)

        # ecg.extend(ecg_track)
        for i in range(len(ecg_track)):
            if len(ecg) > 500:
                ecg.pop(0)
            ecg.append(ecg_track[i])

    print("Thread Ecg ended")


def threadPpg():
    global ppg
    ppg = []

    print("Thread Ppg started")

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    # IP and port for accepting connections
    server_address = ('0.0.0.0', 6661)
    # print server address and port
    print("[+] Server IP {} | Port {}".format(server_address[0], server_address[1]))
    # bind socket with server
    sock.bind(server_address)
    # Listen for incoming connections
    sock.listen(10)
    # Create Loop
    while True:
        # Wait for a connection
        # print('[+]  Waiting for a client connection')
        # connection established
        connection, client_address = sock.accept()
        # print('[+] Connection from', client_address)
        connection, client_address = sock.accept()
        received_data = connection.recv(1024)
        if received_data:
            ppg_sample = int(received_data.decode('utf-8')[2:])
        else:
            ppg_sample = 0
        ppg.extend([ppg_sample])

        if len(ppg) > 150:
            ppg.pop(0)

    print("Thread Ppg ended")


def threadImu():
    global imu   # acc_x, acc_y, acc_z, gyr_x, gyr_y, gyr_z
    imu = {
        'acc_x': [], 
        'acc_y': [],
        'acc_z': [],
        'gyr_x': [],
        'gyr_y': [],
        'gyr_z': []
    }

    print("Thread Imu started")

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    # IP and port for accepting connections
    server_address = ('0.0.0.0', 6662)
    # print server address and port
    print("[+] Server IP {} | Port {}".format(server_address[0], server_address[1]))
    # bind socket with server
    sock.bind(server_address)
    # Listen for incoming connections
    sock.listen(10)
    # Create Loop
    while True:
        # Wait for a connection
        # print('[+]  Waiting for a client connection')
        # connection established
        connection, client_address = sock.accept()
        # print('[+] Connection from', client_address)
        connection, client_address = sock.accept()
        received_data = connection.recv(1024)
        if received_data:
            imu_sample = conv_imu(received_data[2:].decode())

        else:
            imu_sample = [0,0,0,0,0,0,0]
        

        i = 1 # Skip first: header
        for key in imu.keys():
            imu[key].append(imu_sample[i])
            i = i + 1
        
        if len(imu["acc_x"]) > 150:
            for key in imu.keys():
                imu[key].pop(0)
           
    print("Thread Imu ended")

        

class MainApp():
    app = QtGui.QApplication(["Health Monitoring"])
    win = QtGui.QMainWindow()
    widget = QtGui.QWidget()
    win.setCentralWidget(widget)
    layout = QtGui.QGridLayout(widget)
    win.show()
    win.resize(1000, 600)

    w = AnimationWidget()

    timer = QtCore.QTimer()
    timer.timeout.connect(w.update)
    timer.start(0)
    layout.addWidget(w)

    t1 = Thread(target=threadEcg)
    t2 = Thread(target=threadPpg)
    t3 = Thread(target=threadImu)
    t1.start()
    t2.start()
    t3.start()

    app.exec_()

    t1.join()
    t2.join()
    t3.join()

if __name__ == '__main__':
    print("Main start")
    t = MainApp()
    print("Main end")
