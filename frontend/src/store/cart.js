import { defineStore } from 'pinia'
import { cartApi } from '../api'

export const useCartStore = defineStore('cart', {
  state: () => ({ items: [] }),
  getters: {
    count: (s) => s.items.reduce((n, i) => n + i.quantity, 0),
    checkedItems: (s) => s.items.filter((i) => i.checked),
    total: (s) => s.checkedItems.reduce((n, i) => n + Number(i.subtotal), 0)
  },
  actions: {
    async load() { this.items = await cartApi.list() || [] },
    async add(prodId, quantity) { await cartApi.add({ prodId, quantity }); await this.load() },
    async remove(cartId) { await cartApi.remove(cartId); await this.load() },
    async setQty(cartId, q) { await cartApi.updateQty(cartId, q); await this.load() }
  }
})
