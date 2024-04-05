import struct
import datetime
import os

byte_order = "little"

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


def string(chunk, index, len):
	return index+len, chunk[index:index+len].decode("utf-8")


class section_header():

	def __init__(self, chunk, index):
		index, self.sect_code = string(chunk, index, 8)
		index, self.virt_size = uint32(chunk, index)
		index, self.virt_addr = uint32(chunk, index)
		index, self.raw_size = uint32(chunk, index)
		index, self.raw_ptr = uint32(chunk, index)
		index, self.reloc_ptr = uint32(chunk, index)
		index, self.line_ptr = uint32(chunk, index)
		index, self.num_reloc = uint16(chunk, index)
		index, self.num_lines = uint16(chunk, index)

		sect_flags = {
			0x00000000: "Reserved for future use.",
			0x00000001: "Reserved for future use.",
			0x00000002: "Reserved for future use.",
			0x00000004: "Reserved for future use.",
			0x00000008: "The section should not be padded to the next boundary. This flag is obsolete and is replaced by ",
			0x00000010: "Reserved for future use.",
			0x00000020: "The section contains executable code.",
			0x00000040: "The section contains initialized data.",
			0x00000080: "The section contains uninitialized data.",
			0x00000100: "Reserved for future use.",
			0x00000200: "The section contains comments or other information. The .drectve section has this type. This is valid for object files only.",
			0x00000400: "Reserved for future use.",
			0x00000800: "The section will not become part of the image. This is valid only for object files.",
			0x00001000: "The section contains COMDAT data. For more information, see COMDAT Sections (Object Only). This is valid only for object files.",
			0x00008000: "The section contains data referenced through the global pointer (GP).",
			0x00020000: "Reserved for future use.",
			0x00040000: "Reserved for future use.",
			0x00080000: "Reserved for future use.",
			0x00100000: "Align data on a 1-byte boundary. Valid only for object files.",
			0x00200000: "Align data on a 2-byte boundary. Valid only for object files.",
			0x00300000: "Align data on a 4-byte boundary. Valid only for object files.",
			0x00400000: "Align data on an 8-byte boundary. Valid only for object files.",
			0x00500000: "Align data on a 16-byte boundary. Valid only for object files.",
			0x00600000: "Align data on a 32-byte boundary. Valid only for object files.",
			0x00700000: "Align data on a 64-byte boundary. Valid only for object files.",
			0x00800000: "Align data on a 128-byte boundary. Valid only for object files.",
			0x00900000: "Align data on a 256-byte boundary. Valid only for object files.",
			0x00A00000: "Align data on a 512-byte boundary. Valid only for object files.",
			0x00B00000: "Align data on a 1024-byte boundary. Valid only for object files.",
			0x00C00000: "Align data on a 2048-byte boundary. Valid only for object files.",
			0x00D00000: "Align data on a 4096-byte boundary. Valid only for object files.",
			0x00E00000: "Align data on an 8192-byte boundary. Valid only for object files.",
			0x01000000: "The section contains extended relocations.",
			0x02000000: "The section can be discarded as needed.",
			0x04000000: "The section cannot be cached.",
			0x08000000: "The section is not pageable.",
			0x10000000: "The section can be shared in memory.",
			0x20000000: "The section can be executed as code.",
			0x40000000: "The section can be read.",
			0x80000000: "The section can be written to."
		}
		index, self.flags = uint32(chunk, index)
		self.chars = []
		for flag in sect_flags:
			if flag & self.flags:
				self.chars.append(sect_flags[flag])

		self.next_addr = index

	def print(self):
		print(f"Section {self.sect_code} fields")
		print(f"  Virtual size: {self.virt_size}")
		print(f"  Virtual addr: {hex(self.virt_addr)}")
		print(f"  Raw data size: {self.raw_size}")
		print(f"  Raw data ptr: {hex(self.raw_ptr)}")
		print(f"  Number of relocations: {self.num_reloc}")
		print(f"  Pointer to relocations: {hex(self.reloc_ptr)}")
		print(f"  Number of line numbers: {self.num_lines}")
		print(f"  Pointer to line numbers: {hex(self.line_ptr)}")
		print(f"  Characteristics: {self.chars}")
		print()


class img_data_dir():

	def __init__(self, chunk, index, rva_idx):
		table = {
			0: "Export table (.edata)",
			1: "Import table (.idata)",
			2: "Resource table (.rsrc)",
			3: "Exception table (.pdata)",
			4: "Cert table",
			5: "Base reloc table (.reloc)",
			6: "Debug (.debug)",
			7: "Architecture (must be 0)",
			8: "Global ptr (size must be 0)",
			9: "TLS table (.tls)",
			10: "Load config table",
			11: "Bound import",
			12: "Import addr table",
			13: "Delay import descriptor",
			14: "CLR runtime header (.cormeta)",
			15: "Reserved (must be 0)",
		}

		self.rva_type = table[rva_idx]
		index, self.virt_addr = uint32(chunk, index)
		index, self.size = uint32(chunk, index)
		self.next_addr = index

	def print(self):
		print(f"  {self.rva_type}")
		print(f"    Virt address: {hex(self.virt_addr)}")
		print(f"    Size in bytes: {self.size}")


class opt_header():

	def __init__(self, chunk, index):
		opt_head_magic = {
			0x10b: "PE32 executable",
			0x20b: "PE32+ executable",
			0x107: "ROM image"
		}

		index, self.magic = uint16(chunk, index)
		self.exec_type = opt_head_magic[self.magic]
		index, self.maj_linker_ver = uint8(chunk, index)
		index, self.min_linker_ver = uint8(chunk, index)
		index, self.size_of_code = uint32(chunk, index)
		index, self.size_of_init_data = uint32(chunk, index)
		index, self.size_of_uninit_data = uint32(chunk, index)
		index, self.entry_pt_addr = uint32(chunk, index)
		index, self.base_of_code = uint32(chunk, index)

		if self.magic == 0x20b:
			self.base_of_data = 0
			index, self.image_base = uint64(chunk, index)
		else:
			index, self.base_of_data = uint32(chunk, index)
			index, self.image_base = uint32(chunk, index)

		index, self.sect_align = uint32(chunk, index)
		index, self.file_align = uint32(chunk, index)
		index, self.maj_os_ver = uint16(chunk, index)
		index, self.min_os_ver = uint16(chunk, index)
		index, self.maj_img_ver = uint16(chunk, index)
		index, self.min_img_ver = uint16(chunk, index)
		index, self.maj_subsys_ver = uint16(chunk, index)
		index, self.min_subsys_ver = uint16(chunk, index)
		index, self.win32_ver = uint32(chunk, index)
		index, self.size_of_img = uint32(chunk, index)
		index, self.size_of_headers = uint32(chunk, index)
		index, self.checksum = uint32(chunk, index)

		subsys_codes = {
			0: "An unknown subsystem",
			1: "Device drivers and native Windows processes",
			2: "The Windows graphical user interface (GUI) subsystem",
			3: "The Windows character subsystem",
			5: "The OS/2 character subsystem",
			7: "The Posix character subsystem",
			8: "Native Win9x driver",
			9: "Windows CE",
			10: "An Extensible Firmware Interface (EFI) application",
			11: "An EFI driver with boot services",
			12: "An EFI driver with run-time services",
			13: "An EFI ROM image",
			14: "XBOX",
			16: "Windows boot application"
		}
		index, subsys = uint16(chunk, index)
		self.subsys_type = subsys_codes[subsys]

		dll_flag_codes = {
			0x0001: "Reserved, must be zero.",
			0x0002: "Reserved, must be zero.",
			0x0004: "Reserved, must be zero.",
			0x0008: "Reserved, must be zero.",
			0x0020: "Image can handle a high entropy 64-bit virtual address space.",
			0x0040: "DLL can be relocated at load time.",
			0x0080: "Code Integrity checks are enforced.",
			0x0100: "Image is NX compatible.",
			0x0200: "Isolation aware, but do not isolate the image.",
			0x0400: "Does not use structured exception (SE) handling. No SE handler may be called in this image.",
			0x0800: "Do not bind the image.",
			0x1000: "Image must execute in an AppContainer.",
			0x2000: "A WDM driver.",
			0x4000: "Image supports Control Flow Guard.",
			0x8000: "Terminal Server aware."
		}
		index, dll_flags = uint16(chunk, index)
		self.dll_chars = []
		for flag in dll_flag_codes:
			if dll_flags & flag:
				self.dll_chars.append(dll_flag_codes[flag])

		if self.magic == 0x20b:
			index, self.size_of_stack_res = uint64(chunk, index)
			index, self.size_of_stack_comm = uint64(chunk, index)
			index, self.size_of_heap_res = uint64(chunk, index)
			index, self.size_of_heap_comm = uint64(chunk, index)
		else:
			index, self.size_of_stack_res = uint32(chunk, index)
			index, self.size_of_stack_comm = uint32(chunk, index)
			index, self.size_of_heap_res = uint32(chunk, index)
			index, self.size_of_heap_comm = uint32(chunk, index)

		index, self.load_flags = uint32(chunk, index)
		index, self.num_rva = uint32(chunk, index)

		self.rvas = []
		if self.num_rva > 0:
			for rva_idx in range(self.num_rva):
				rva = img_data_dir(chunk, index, rva_idx)
				index = rva.next_addr
				self.rvas.append(rva)

		self.sect_head_idx = index

	def print(self):
		print("Optional header fields")
		print(f"  Executable type: {self.exec_type}")
		print(f"  Major linker version: {self.maj_linker_ver}")
		print(f"  Minor linker version: {self.min_linker_ver}")
		print(f"  Size of code: {self.size_of_code}")
		print(f"  Size of initialized data: {self.size_of_init_data}")
		print(f"  Size of uninitialized data: {self.size_of_uninit_data}")
		print(f"  Addr of entry point: {hex(self.entry_pt_addr)}")
		print(f"  Base of code: {hex(self.base_of_code)}")
		print(f"  Base of data: {hex(self.base_of_data)}")
		print(f"  Image base: {hex(self.image_base)}")
		print(f"  Section Alignment: {hex(self.sect_align)}")
		print(f"  File Alignment: {hex(self.file_align)}")
		print(f"  Major OS version: {self.maj_os_ver}")
		print(f"  Minor OS version: {self.min_os_ver}")
		print(f"  Major image version: {self.maj_img_ver}")
		print(f"  Minor image version: {self.min_img_ver}")
		print(f"  Major subsystem version: {self.maj_subsys_ver}")
		print(f"  Minor subsystem version: {self.min_subsys_ver}")
		print(f"  Win32 version: {self.win32_ver}")
		print(f"  Size of image: {hex(self.size_of_img)}")
		print(f"  Size of headers: {hex(self.size_of_headers)}")
		print(f"  Checksum: {self.checksum}")
		print(f"  Subsystem: {self.subsys_type}")
		print(f"  Dll Characteristics: {self.dll_chars}")
		print(f"  Size of stack reserve: {self.size_of_stack_res}")
		print(f"  Size of stack commit: {self.size_of_stack_comm}")
		print(f"  Size of heap reserve: {self.size_of_heap_res}")
		print(f"  Size of heap commit: {self.size_of_heap_comm}")
		print(f"  Loader flags: {self.load_flags}")
		print(f"  Number of RVA/size pairs: {self.num_rva}")
		for rva_idx in range(self.num_rva):
			self.rvas[rva_idx].print()
		print()


class nt_header():

	def __init__(self, chunk, index):
		global byte_order
		byte_order = "big"
		index, self.magic = uint32(chunk, index)
		byte_order = "little"

		machine_targets = {
			0x0: "Unknown",
			0x184: "Alpha AXP, 32-bit address space",
			0x284: "Alpha 64, 64-bit address space",
			0x1d3: "Matsushita AM33",
			0x8664: "x64",
			0x1c0: "ARM little endian",
			0xaa64: "ARM64 little endian",
			0x1c4: "ARM Thumb-2 little endian",
			0x284: "AXP 64 (Same as Alpha 64)",
			0xebc: "EFI byte code",
			0x14c: "Intel 386 or later processors and compatible processors",
			0x200: "Intel Itanium processor family",
			0x6232: "LoongArch 32-bit processor family",
			0x6264: "LoongArch 64-bit processor family",
			0x9041: "Mitsubishi M32R little endian",
			0x266: "MIPS16",
			0x366: "MIPS with FPU",
			0x466: "MIPS16 with FPU",
			0x1f0: "Power PC little endian",
			0x1f1: "Power PC with floating point support",
			0x166: "MIPS little endian",
			0x5032: "RISC-V 32-bit address space",
			0x5064: "RISC-V 64-bit address space",
			0x5128: "RISC-V 128-bit address space",
			0x1a2: "Hitachi SH3",
			0x1a3: "Hitachi SH3 DSP",
			0x1a6: "Hitachi SH4",
			0x1a8: "Hitachi SH5",
			0x1c2: "Thumb",
			0x169: "MIPS little-endian WCE v2"
		}

		index, self.tgt_machine_code = uint16(chunk, index)
		self.tgt_machine = machine_targets[self.tgt_machine_code]
		index, self.num_sections = uint16(chunk, index)
		index, self.time_date = uint32(chunk, index)
		index, self.sym_table_ptr = uint32(chunk, index)
		index, self.num_symbols = uint32(chunk, index)
		index, self.opt_header_size = uint16(chunk, index)

		flag_codes = {
			0x0001: "Image only, Windows CE, and Microsoft Windows NT and later. This indicates that the file does not contain base relocations and must therefore be loaded at its preferred base address. If the base address is not available, the loader reports an error. The default behavior of the linker is to strip base relocations from executable (EXE) files.",
			0x0002: "Image only. This indicates that the image file is valid and can be run. If this flag is not set, it indicates a linker error.",
			0x0004: "COFF line numbers have been removed. This flag is deprecated and should be zero.",
			0x0008: "COFF symbol table entries for local symbols have been removed. This flag is deprecated and should be zero.",
			0x0010: "Obsolete. Aggressively trim working set. This flag is deprecated for Windows 2000 and later and must be zero.",
			0x0020: "Application can handle > 2-GB addresses.",
			0x0040: "This flag is reserved for future use.",
			0x0080: "Little endian: the least significant bit (LSB) precedes the most significant bit (MSB) in memory. This flag is deprecated and should be zero.",
			0x0100: "Machine is based on a 32-bit-word architecture.",
			0x0200: "Debugging information is removed from the image file.",
			0x0400: "If the image is on removable media, fully load it and copy it to the swap file.",
			0x0800: "If the image is on network media, fully load it and copy it to the swap file.",
			0x1000: "The image file is a system file, not a user program.",
			0x2000: "The image file is a dynamic-link library (DLL). Such files are considered executable files for almost all purposes, although they cannot be directly run.",
			0x4000: "The file should be run only on a uniprocessor machine.",
			0x8000: "Big endian: the MSB precedes the LSB in memory. This flag is deprecated and should be zero."
		}

		index, self.flags = uint16(chunk, index)
		self.chars = []
		for flag in flag_codes:
			if self.flags & flag:
				self.chars.append(flag_codes[flag])

		self.next_header_addr = index

	def print(self):
		print("NT header fields")
		print(f"  Magic number: {hex(self.magic)}")
		print(f"  Target machine: {self.tgt_machine}")
		print(f"  Number of sections: {self.num_sections}")
		print(f"  Date & time of creation: {datetime.datetime.fromtimestamp(self.time_date)}")
		print(f"  Pointer to sym table: {self.sym_table_ptr}")
		print(f"  Number of symbols: {self.num_symbols}")
		print(f"  Size of optional header: {self.opt_header_size}")
		print(f"  Characteristics: {self.chars}")
		print()


class rich_header():

	def __init__(self):
		pass

	def print(self):
		pass


class dos_stub():

	def __init__(self):
		pass

	def print(self):
		pass


class dos_header():

	def __init__(self, chunk):
		index = 0
		index, self.magic = uint16(chunk, index)
		index, self.last_page_bytes = uint16(chunk, index)
		index, self.num_pages = uint16(chunk, index)
		index, self.relocations = uint16(chunk, index)
		index, self.para_size = uint16(chunk, index)
		index, self.min_alloc = uint16(chunk, index)
		index, self.max_alloc = uint16(chunk, index)
		index, self.ss_init = uint16(chunk, index)
		index, self.sp_init = uint16(chunk, index)
		index, self.checksum = uint16(chunk, index)
		index, self.ip_init = uint16(chunk, index)
		index, self.cs_init = uint16(chunk, index)
		index, self.reloc_addr = uint16(chunk, index)
		index, self.ovrlay_num = uint16(chunk, index)
		index, res1 = uint16(chunk, index)
		index, res2 = uint16(chunk, index)
		index, res3 = uint16(chunk, index)
		index, res4 = uint16(chunk, index)
		self.res_words = [res1, res2, res3, res4]
		index, self.oem_id = uint16(chunk, index)
		index, self.oem_info = uint16(chunk, index)
		index, res5 = uint16(chunk, index)
		index, res6 = uint16(chunk, index)
		index, res7 = uint16(chunk, index)
		index, res8 = uint16(chunk, index)
		index, res9 = uint16(chunk, index)
		index, res10 = uint16(chunk, index)
		index, res11 = uint16(chunk, index)
		index, res12 = uint16(chunk, index)
		index, res13 = uint16(chunk, index)
		index, res14 = uint16(chunk, index)
		self.res_words2 = [res5, res6, res7, res8, res9, res10, res11, res12, res13, res14]
		index, self.nt_header_addr = uint16(chunk, index)

	def print(self):
		print("DOS header fields")
		print(f"  Magic number: {hex(self.magic)}")
		print(f"  Number of bytes on last page: {self.last_page_bytes}")
		print(f"  Number of pages in file: {self.num_pages}")
		print(f"  Relocations: {self.relocations}")
		print(f"  Size of header in paragraphs: {self.para_size}")
		print(f"  Min extra paragraphs needed: {self.min_alloc}")
		print(f"  Max extra paragraphs needed: {self.max_alloc}")
		print(f"  Initial SS value: {self.ss_init}")
		print(f"  Initial SP value: {self.sp_init}")
		print(f"  Checksum: {self.checksum}")
		print(f"  Initial IP value: {self.ip_init}")
		print(f"  Initial CS value: {self.cs_init}")
		print(f"  File addr of reloc table: {hex(self.reloc_addr)}")
		print(f"  Overlay number: {self.ovrlay_num}")
		print(f"  Reserved words[4]: {self.res_words}")
		print(f"  OEM ID: {self.oem_id}")
		print(f"  OEM info: {self.oem_info}")
		print(f"  Reserved words[10]: {self.res_words2}")
		print(f"  File addr of NT header: {hex(self.nt_header_addr)}")
		print()

#file_path = "C:\\Program Files\\7-Zip\\7z.exe"
file_path = "C:\\cygwin64\\home\\Admin\\hw.exe"
#file_path = "C:\\cygwin64\\home\\Admin\\prime.exe"

with open(file_path, "rb") as file:
	chunk = file.read(os.path.getsize(file_path))
	dos_head = dos_header(chunk)
	dos_head.print()
	nt_head = nt_header(chunk, dos_head.nt_header_addr)
	nt_head.print()
	section_header_index = 0
	sect_headers = []
	if nt_head.opt_header_size > 0:
		opt_head = opt_header(chunk, nt_head.next_header_addr)
		opt_head.print()
		section_header_index = opt_head.sect_head_idx
	else:
		section_header_index = nt_head.next_header_addr
	for sect_idx in range(nt_head.num_sections):
		sect_head = section_header(chunk, section_header_index)
		section_header_index = sect_head.next_addr
		sect_headers.append(sect_head)
		sect_head.print()
