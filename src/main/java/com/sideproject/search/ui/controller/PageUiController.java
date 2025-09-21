package com.sideproject.search.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageUiController {
	
	@GetMapping("/")
	public String index() {
		return "index";
	}
}
