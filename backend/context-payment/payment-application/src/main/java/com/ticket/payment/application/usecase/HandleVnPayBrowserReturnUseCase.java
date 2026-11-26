package com.ticket.payment.application.usecase;

import java.util.Map;

public interface HandleVnPayBrowserReturnUseCase {
    String execute(Map<String, String> allParams);
}
