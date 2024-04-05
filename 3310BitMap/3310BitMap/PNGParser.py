from tkinter import *
import zlib
import time
import threading

byte_order = "little"
img_width = 0
img_height = 0

def uint(chunk):
	return int.from_bytes(chunk, byteorder=byte_order, signed=False)


def uint8(chunk, index):
	return index+1, uint(chunk[index:index+1])


def uint16(chunk, index):
	return index+2, uint(chunk[index:index+2])


def uint32(chunk, index):
	return index+4, uint(chunk[index:index+4])


def uint64(chunk, index):
	return index+8, uint(chunk[index:index+8])


def type(chunk, index):
    return index+4, chunk[index:index+4].decode("utf-8")


class IHDR:
    def __init__(self, file, chunk_length):
        chunk = file.read(chunk_length + 4)
        index = 0

        index, self.width = uint32(chunk, index)
        index, self.height = uint32(chunk, index)
        print(f"Image scale is {self.width}x{self.height} pixels")

    def print(self):
        pass


def _from_rgb(rgb):
    """translates an rgb tuple of int to a tkinter friendly color code
    """
    return "#%02x%02x%02x" % rgb


class IDAT:
    def __init__(self, file, chunk_length):
        chunk = file.read(chunk_length + 4)
        index = 0

        chunk_data = chunk[index:index + chunk_length]
        self.data = zlib.decompress(chunk_data, zlib.MAX_WBITS)
        with open("C:\\Users\\Admin\\Desktop\\png.bin", 'wb') as out_file:
            out_file.write(self.data)
        print(len(self.data))

    def show(self):
        global img_width
        global img_height
        print(img_width * img_height)

        root = Tk()
        root.geometry(f"{img_width*3+100}x{img_height*3+100}")
        #root.option_add("*tearOff", FALSE)
        canvas = Canvas(root, width=img_width*3, height=img_height*3, background='black')
        canvas.place(x=50, y=50, width=img_width*3, height=img_height*3, anchor=NW)

        def draw_img():
            index = 0
            for h in range(img_height):
                index, mystery = uint8(self.data, index)
                #print(hex(mystery))
                for w in range(img_width):
                    index, r = uint8(self.data, index)
                    index, g = uint8(self.data, index)
                    index, b = uint8(self.data, index)
                    index, a = uint8(self.data, index)
                    #if a == 0xff and r == 0xff and g == 0xff and b == 0xff:
                    #    continue

                    canvas.create_rectangle(w*3, h*3, w*3 + 3, h*3 + 3, fill=_from_rgb((r, g, b)), outline=_from_rgb((r, g, b)))
                    #canvas.create_rectangle(0, 0, img_width*3, img_height*3, fill=_from_rgb((r, g, b)), outline=_from_rgb((r, g, b)))
                    #canvas.create_text(100, 100, text=f"{a}, {r}, {g}, {b}", fill='black')
                    #canvas.create_rectangle(w, h, w + 1, h + 1, fill='red', outline='red')
                    #print(f"Pixel #{index / 3} has color {hex(r)}{hex(g)}{hex(b)}")
            print("Drawing complete")
        draw = threading.Thread(target=draw_img)
        draw.start()

        root.mainloop()


def read_chunk(file):
    chunk = file.read(8)
    if not chunk:
        exit(0)
    index = 0

    global byte_order
    byte_order = "big"
    index, chunk_length = uint32(chunk, index)
    print(chunk_length)

    index, chunk_type = type(chunk, index)
    print(chunk_type)

    if chunk_type == "IHDR":
        header = IHDR(file, chunk_length)
        header.print()
        global img_width
        global img_height
        img_width = header.width
        img_height = header.height
        return

    if chunk_type == "IDAT":
        image = IDAT(file, chunk_length)
        image.show()
        return

    chunk = file.read(chunk_length + 4)
    index = 0

    chunk_data = chunk[index:index + chunk_length]
    print(chunk_data)
    index += chunk_length

    index, chunk_crc = uint32(chunk, index)
    print(chunk_crc)


filePath = "C:\\users\\Admin\\Desktop\\utpb.png"

with open(filePath, 'rb') as file:
    chunk = file.read(8)
    index = 0

    index, magic = uint64(chunk, 0)

    if magic != 727905341920923785:
        print("Not a valid PNG file!")
        exit(0)

    while True:
        read_chunk(file)


