import pandas as pd

# DECODING FUNCTIONS
def convert_array_to_signed_int(data, offset, length):
    return int.from_bytes(
        bytearray(data[offset: offset + length]), byteorder="little", signed=True,
    )


def convert_to_unsigned_long(data, offset, length):
    return int.from_bytes(
        bytearray(data[offset: offset + length]), byteorder="little", signed=False,
    )


def conv_ecg(data):
    ecg_session_data = []
    ecg_session_time = []
    if len(data)>0:
        tmp = data[0]
    else:
        tmp = 0x00

    if tmp == 0x00:
        timestamp = convert_to_unsigned_long(data, 1, 8)
        step = 3
        samples = data[10:] # Check 10 ?
        offset = 0
        while offset < len(samples):
            ecg = convert_array_to_signed_int(samples, offset, step)
            offset += step
            ecg_session_data.extend([ecg])
            ecg_session_time.extend([timestamp])
    return ecg_session_data


def conv_imu(data):
    imu_sample = []
    for byte_idx in range(len(data)//4):
        data_slice = data[byte_idx*4:(byte_idx*4+4)]
        byte_data = bytes.fromhex(data_slice)
        value = float(int.from_bytes(byte_data, byteorder = 'little', signed = True))
        imu_sample.append(value)
    return imu_sample
