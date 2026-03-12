{
  "yc.ecomigo": {
    "aggregate": {
      "assistencia.atendimento": {
        "org": {
          "name": "yc"
        },
        "project": {
          "name": "ecomigo"
        },
        "schema": {
          "forWriteModel.name": "t_ae_db.client",
          "forReadModel.name": "assistencia"
        },
        "boundedContext": {
          "name": "assistencia",
          "comment": "Gestão de Atendimentos"
        },
        "tenantId": {
          "forWriteModel": "9d77f828-5e8c-4807-b554-5a29e85fc37f",
          "forReadModel": "9d77f828-5e8c-4807-b554-5a29e85fc37f"
        },
        "type": "atendimento",
        "command": {
          "solicitar": {
            "data": {
              "attribute": {
                "tipodeocorrencia": {
                  "type": "String",
                  "length": 100,
                  "nullable": false,
                  "comment": "Tipo de ocorrência reportada pelo cliente"
                },
                "protocolo": {
                  "type": "String",
                  "length": 64,
                  "nullable": false,
                  "comment": "Chave de protocolo de atendimento"
                },
                "status": {
                  "type": "String",
                  "length": 32,
                  "nullable": false,
                  "comment": "Estado atual do agregado"
                }
              },
              "valueObject": {
                "single": {
                  "cliente": {
                    "id": {
                      "type": "Long",
                      "nullable": false,
                      "comment": "ID do cliente"
                    },
                    "nome": {
                      "type": "String",
                      "length": 200,
                      "nullable": false,
                      "comment": "Nome completo do cliente"
                    },
                    "docfiscal": {
                      "type": "Json",
                      "nullable": false,
                      "comment": "Documento fiscal (tipo e numero)"
                    }
                  },
                  "veiculo": {
                    "placa": {
                      "type": "String",
                      "length": 10,
                      "nullable": false,
                      "comment": "Placa do veículo"
                    }
                  },
                  "servico": {
                    "id": {
                      "type": "Long",
                      "nullable": false,
                      "comment": "ID do serviço"
                    },
                    "nome": {
                      "type": "String",
                      "length": 100,
                      "nullable": false,
                      "comment": "Nome do serviço"
                    }
                  },
                  "base": {
                    "tipo": {
                      "type": "String",
                      "length": 20,
                      "nullable": false,
                      "comment": "Tipo de endereço (RESIDENCIAL, COMERCIAL, etc)"
                    },
                    "logradouro": {
                      "type": "String",
                      "length": 200,
                      "nullable": false,
                      "comment": "Logradouro do endereço"
                    },
                    "numero": {
                      "type": "String",
                      "length": 10,
                      "nullable": false,
                      "comment": "Número do endereço"
                    },
                    "complemento": {
                      "type": "String",
                      "length": 100,
                      "nullable": true,
                      "comment": "Complemento do endereço"
                    },
                    "bairro": {
                      "type": "String",
                      "length": 100,
                      "nullable": false,
                      "comment": "Bairro do endereço"
                    },
                    "cidade": {
                      "type": "String",
                      "length": 100,
                      "nullable": false,
                      "comment": "Cidade do endereço"
                    },
                    "estado": {
                      "type": "String",
                      "length": 2,
                      "nullable": false,
                      "comment": "Estado (UF) do endereço"
                    },
                    "cep": {
                      "type": "String",
                      "length": 8,
                      "nullable": false,
                      "comment": "CEP sem formatação (apenas dígitos)"
                    }
                  },
                  "origem": {
                    "tipo": {
                      "type": "String",
                      "length": 20,
                      "nullable": false,
                      "comment": "Tipo de endereço (RESIDENCIAL, COMERCIAL, etc)"
                    },
                    "logradouro": {
                      "type": "String",
                      "length": 200,
                      "nullable": false,
                      "comment": "Logradouro do endereço"
                    },
                    "numero": {
                      "type": "String",
                      "length": 10,
                      "nullable": false,
                      "comment": "Número do endereço"
                    },
                    "complemento": {
                      "type": "String",
                      "length": 100,
                      "nullable": true,
                      "comment": "Complemento do endereço"
                    },
                    "bairro": {
                      "type": "String",
                      "length": 100,
                      "nullable": false,
                      "comment": "Bairro do endereço"
                    },
                    "cidade": {
                      "type": "String",
                      "length": 100,
                      "nullable": false,
                      "comment": "Cidade do endereço"
                    },
                    "estado": {
                      "type": "String",
                      "length": 2,
                      "nullable": false,
                      "comment": "Estado (UF) do endereço"
                    },
                    "cep": {
                      "type": "String",
                      "length": 8,
                      "nullable": false,
                      "comment": "CEP sem formatação (apenas dígitos)"
                    }
                  }
                },
                "multiple": {
                  
                }
              }
            },
            "endState": "solicitado",
            "roles": [
              "MASTER",
              "ATENDENTE",
              "GERENTE"
            ],
            "fromState": [
              
            ],
            "coordination": {
              
            },
            "br": {
              
            }
          },
          "ajustar": {
            "data": {
              "attribute": {
                "descricao": {
                  "type": "Text",
                  "nullable": true,
                  "comment": "Descrição do ajuste realizado"
                },
                "status": {
                  "type": "String",
                  "length": 32,
                  "nullable": false,
                  "comment": "Estado atual do agregado"
                }
              },
              "valueObject": {
                "single": {
                  "prestador": {
                    "id": {
                      "type": "Long",
                      "nullable": false,
                      "comment": "ID do prestador"
                    },
                    "nome": {
                      "type": "String",
                      "length": 200,
                      "nullable": false,
                      "comment": "Nome completo do prestador"
                    },
                    "docfiscal": {
                      "type": "Json",
                      "nullable": false,
                      "comment": "Documento fiscal (tipo e numero)"
                    }
                  },
                  "servico": {
                    "id": {
                      "type": "Long",
                      "nullable": true,
                      "comment": "ID do serviço"
                    },
                    "nome": {
                      "type": "String",
                      "length": 100,
                      "nullable": true,
                      "comment": "Nome do serviço"
                    }
                  },
                  "origem": {
                    "tipo": {
                      "type": "String",
                      "length": 20,
                      "nullable": true,
                      "comment": "Tipo de endereço (RESIDENCIAL, COMERCIAL, etc)"
                    },
                    "logradouro": {
                      "type": "String",
                      "length": 200,
                      "nullable": true,
                      "comment": "Logradouro do endereço"
                    },
                    "numero": {
                      "type": "String",
                      "length": 10,
                      "nullable": true,
                      "comment": "Número do endereço"
                    },
                    "complemento": {
                      "type": "String",
                      "length": 100,
                      "nullable": true,
                      "comment": "Complemento do endereço"
                    },
                    "bairro": {
                      "type": "String",
                      "length": 100,
                      "nullable": true,
                      "comment": "Bairro do endereço"
                    },
                    "cidade": {
                      "type": "String",
                      "length": 100,
                      "nullable": true,
                      "comment": "Cidade do endereço"
                    },
                    "estado": {
                      "type": "String",
                      "length": 2,
                      "nullable": true,
                      "comment": "Estado (UF) do endereço"
                    },
                    "cep": {
                      "type": "String",
                      "length": 8,
                      "nullable": true,
                      "comment": "CEP sem formatação (apenas dígitos)"
                    }
                  },
                  "destino": {
                    "tipo": {
                      "type": "String",
                      "length": 20,
                      "nullable": false,
                      "comment": "Tipo de endereço (RESIDENCIAL, COMERCIAL, etc)"
                    },
                    "logradouro": {
                      "type": "String",
                      "length": 200,
                      "nullable": false,
                      "comment": "Logradouro do endereço"
                    },
                    "numero": {
                      "type": "String",
                      "length": 10,
                      "nullable": false,
                      "comment": "Número do endereço"
                    },
                    "complemento": {
                      "type": "String",
                      "length": 100,
                      "nullable": true,
                      "comment": "Complemento do endereço"
                    },
                    "bairro": {
                      "type": "String",
                      "length": 100,
                      "nullable": false,
                      "comment": "Bairro do endereço"
                    },
                    "cidade": {
                      "type": "String",
                      "length": 100,
                      "nullable": false,
                      "comment": "Cidade do endereço"
                    },
                    "estado": {
                      "type": "String",
                      "length": 2,
                      "nullable": false,
                      "comment": "Estado (UF) do endereço"
                    },
                    "cep": {
                      "type": "String",
                      "length": 8,
                      "nullable": false,
                      "comment": "CEP sem formatação (apenas dígitos)"
                    }
                  }
                },
                "multiple": {
                  "items": {
                    "nome": {
                      "type": "String",
                      "length": 200,
                      "nullable": false,
                      "comment": "Nome do item de serviço"
                    },
                    "unidadedemedida": {
                      "type": "String",
                      "length": 20,
                      "nullable": false,
                      "comment": "Unidade de medida (ex: unidade, km, hora)"
                    },
                    "precounitario": {
                      "type": "Integer",
                      "nullable": false,
                      "comment": "Preço unitário em centavos"
                    },
                    "quantidade": {
                      "type": "Integer",
                      "nullable": false,
                      "comment": "Quantidade do item"
                    },
                    "observacao": {
                      "type": "String",
                      "length": 500,
                      "nullable": true,
                      "comment": "Observações sobre o item"
                    }
                  }
                }
              }
            },
            "endState": "ajustado",
            "roles": [
              "MASTER",
              "ATENDENTE",
              "GERENTE"
            ],
            "fromState": [
              "solicitado",
              "ajustado"
            ],
            "coordination": {
              
            },
            "br": {
              
            }
          },
          "confirmar": {
            "data": {
              "attribute": {
                "status": {
                  "type": "String",
                  "length": 32,
                  "nullable": false,
                  "comment": "Estado atual do agregado"
                }
              },
              "valueObject": {
                "single": {
                  
                },
                "multiple": {
                  
                }
              }
            },
            "endState": "confirmado",
            "roles": [
              "MASTER",
              "ATENDENTE",
              "GERENTE"
            ],
            "fromState": [
              "ajustado",
              "solicitado"
            ],
            "coordination": {
              
            },
            "br": {
              
            }
          },
          "ocorrencia": {
            "data": {
              "attribute": {
                "status": {
                  "type": "String",
                  "length": 32,
                  "nullable": false,
                  "comment": "Estado atual do agregado"
                }
              },
              "valueObject": {
                "single": {
                  
                },
                "multiple": {
                  "ocorrencias": {
                    "type": "Text",
                    "nullable": false,
                    "comment": "Descrição da ocorrência registrada"
                  }
                }
              }
            },
            "endState": "ocorrido",
            "roles": [
              "MASTER",
              "ATENDENTE",
              "GERENTE"
            ],
            "fromState": [
              "confirmado",
              "ocorrido"
            ],
            "coordination": {
              
            },
            "br": {
              
            }
          },
          "finalizar": {
            "data": {
              "attribute": {
                "status": {
                  "type": "String",
                  "length": 32,
                  "nullable": false,
                  "comment": "Estado atual do agregado"
                }
              },
              "valueObject": {
                "single": {
                  
                },
                "multiple": {
                  
                }
              }
            },
            "endState": "finalizado",
            "roles": [
              "MASTER",
              "ATENDENTE",
              "GERENTE"
            ],
            "fromState": [
              "confirmado",
              "ocorrido"
            ],
            "coordination": {
              
            },
            "br": {
              
            }
          },
          "cancelar": {
            "data": {
              "attribute": {
                "status": {
                  "type": "String",
                  "length": 32,
                  "nullable": false,
                  "comment": "Estado atual do agregado"
                }
              },
              "valueObject": {
                "single": {
                  
                },
                "multiple": {
                  
                }
              }
            },
            "endState": "cancelado",
            "roles": [
              "MASTER",
              "ATENDENTE",
              "GERENTE"
            ],
            "fromState": [
              "solicitado",
              "ajustado"
            ],
            "coordination": {
              
            },
            "br": {
              
            }
          }
        },
        "event": {
          "solicitado": {
            "type": "solicitado",
            "whenAttribute": "solicitadoem",
            "domainBus": {
              "triggerProjection": [
                "persistence/c/e"
              ]
            }
          },
          "ajustado": {
            "type": "ajustado",
            "whenAttribute": "ajustadoem",
            "domainBus": {
              "triggerProjection": [
                "persistence/c/e"
              ],
              "triggerDomain": [
                {
                  "boundedContext": "suporte",
                  "aggregate": "atendimento",
                  "event": "ajustado",
                  "enabled": "false"
                }
              ]
            }
          },
          "confirmado": {
            "type": "confirmado",
            "whenAttribute": "confirmadoem",
            "domainBus": {
              "triggerProjection": [
                "persistence/c/e"
              ],
              "triggerDomain": [
                {
                  "boundedContext": "suporte",
                  "aggregate": "atendimento",
                  "event": "confirmado",
                  "enabled": "false"
                }
              ]
            }
          },
          "ocorrido": {
            "type": "ocorrido",
            "whenAttribute": "ocorridoem",
            "domainBus": {
              "triggerProjection": [
                "persistence/c/e"
              ]
            }
          },
          "finalizado": {
            "type": "finalizado",
            "whenAttribute": "finalizadoem",
            "domainBus": {
              "triggerProjection": [
                "persistence/c/e"
              ],
              "triggerDomain": [
                {
                  "boundedContext": "suporte",
                  "aggregate": "atendimento",
                  "event": "finalizado",
                  "enabled": "false"
                },
                {
                  "boundedContext": "financeiro",
                  "aggregate": "atendimento",
                  "event": "finalizado"
                }
              ]
            }
          },
          "cancelado": {
            "type": "cancelado",
            "whenAttribute": "canceladoem",
            "domainBus": {
              "triggerProjection": [
                "persistence/c/e"
              ],
              "triggerDomain": [
                {
                  "boundedContext": "suporte",
                  "aggregate": "atendimento",
                  "event": "cancelado",
                  "enabled": "false"
                }
              ]
            }
          }
        }
      }
    }
  }
}
