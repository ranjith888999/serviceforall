package com.srr.myapplication.util

import com.srr.myapplication.model.Category
import com.srr.myapplication.model.Product
import com.srr.myapplication.model.SubCategory

object DummyData {
    val categories = listOf(
        Category("cat_cctv", "CCTV", null, listOf(
            SubCategory("sub_hd", "HD Cameras", "cat_cctv"),
            SubCategory("sub_ip", "IP Cameras", "cat_cctv"),
            SubCategory("sub_wifi", "Wi-Fi Cameras", "cat_cctv"),
            SubCategory("sub_4g", "4G Cameras", "cat_cctv"),
            SubCategory("sub_4g_solar", "4G Solar Camera", "cat_cctv")
        )),
        Category("cat_epabx", "EPABX", null, listOf(
            SubCategory("sub_analog", "Analog EPABX or Intercom", "cat_epabx"),
            SubCategory("sub_digital", "Digital EPABX", "cat_epabx"),
            SubCategory("sub_ip_pbx", "IP PBX", "cat_epabx")
        )),
        Category("cat_biometric", "Biometric", null, listOf(
            SubCategory("sub_bio_att", "Biometric with Attendance", "cat_biometric"),
            SubCategory("sub_bio_door", "Biometric Door Access with Attendance", "cat_biometric")
        )),
        Category("cat_networking", "Networking", null, listOf(
            SubCategory("sub_racks", "Racks with Accessories", "cat_networking"),
            SubCategory("sub_routers", "Routers", "cat_networking"),
            SubCategory("sub_modems", "Modems", "cat_networking"),
            SubCategory("sub_switches", "Switches", "cat_networking"),
            SubCategory("sub_firewalls", "Firewalls", "cat_networking"),
            SubCategory("sub_patch", "Patch Panels", "cat_networking"),
            SubCategory("sub_io", "IO, Box, Face Plates", "cat_networking"),
            SubCategory("sub_cables_net", "Cables", "cat_networking")
        )),
        Category("cat_cables", "Cables", null, listOf(
            SubCategory("sub_cctv_c", "CCTV", "cat_cables"),
            SubCategory("sub_telecom", "Telecom", "cat_cables"),
            SubCategory("sub_network_c", "Network", "cat_cables"),
            SubCategory("sub_ofc", "OFC", "cat_cables")
        )),
        Category("cat_it", "Desktop & Laptop", null, listOf(
            SubCategory("sub_desktop", "Desktop", "cat_it"),
            SubCategory("sub_laptop", "Laptop", "cat_it"),
            SubCategory("sub_mac", "Mac", "cat_it"),
            SubCategory("sub_monitor", "Monitor", "cat_it")
        )),
        Category("cat_printers", "Printers", null, listOf(
            SubCategory("sub_bw_laser", "B/W Laser Printer", "cat_printers"),
            SubCategory("sub_inkjet", "Inkjet Printers", "cat_printers"),
            SubCategory("sub_aio", "All in One Printers", "cat_printers")
        )),
        Category("cat_wlc", "Water Level Controllers", null, listOf(
            SubCategory("sub_auto_wlc", "Automatic Water Level Controller", "cat_wlc")
        ))
    )

    // Generate products based on subcategories for search to work
    val allProducts = categories.flatMap { cat ->
        cat.subCategories.map { sub ->
            Product(
                id = sub.id,
                name = sub.name,
                description = "High quality ${sub.name} for your needs.",
                categoryId = cat.id,
                subCategoryId = sub.id,
                imageUrl = "defaultimage"
            )
        }
    }
}
