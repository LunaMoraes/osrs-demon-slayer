"""Offline fixtures for the Wiki monster-data generator."""

import unittest

from generate_monsters import make_index


class MonsterGeneratorTest(unittest.TestCase):
    def test_multiple_ids_attributes_and_boss_category(self):
        rows = [{"page_name": "Vorkath", "name": "Vorkath", "id": ["8060", "8061"],
                 "attribute": ["dragon", "undead"], "combat_level": 732},
                {"page_name": "Ordinary", "name": "Ordinary", "id": ["9"],
                 "attribute": ["dragon"], "combat_level": 10}]
        index, excluded = make_index(rows, [rows[0], rows[1]])
        self.assertEqual(["8060", "8061"], list(index))
        self.assertEqual(["undead"], index["8061"]["attributes"])
        self.assertTrue(index["8061"]["boss"])
        self.assertEqual(["Ordinary"], excluded)

    def test_same_page_duplicate_id_uses_lower_level(self):
        rows = [{"page_name": "Boss", "name": "Boss", "id": ["1"],
                 "attribute": ["demon"], "combat_level": 100},
                {"page_name": "Boss", "name": "Boss", "id": ["1"],
                 "attribute": ["demon"], "combat_level": 90}]
        index, _ = make_index(rows, rows)
        self.assertEqual(90, index["1"]["level"])

    def test_conflicting_id_fails(self):
        rows = [{"page_name": "A", "name": "A", "id": ["1"],
                 "attribute": ["demon"], "combat_level": 10},
                {"page_name": "B", "name": "B", "id": ["1"],
                 "attribute": ["undead"], "combat_level": 10}]
        with self.assertRaises(ValueError):
            make_index(rows, [])

    def test_vampyres_and_wilderness_zombie_pirates(self):
        rows = [{"page_name": "Feral vampyre", "name": "Feral vampyre", "id": ["3237"],
                 "attribute": ["vampyre1"], "combat_level": 61},
                {"page_name": "Zombie pirate", "name": "Zombie pirate", "id": ["13489"],
                 "attribute": [], "combat_level": 22}]
        index, _ = make_index(rows, [])
        self.assertEqual(["vampire"], index["3237"]["attributes"])
        self.assertEqual(["undead"], index["13489"]["attributes"])

    def test_black_demon_remains_normal_when_wiki_boss_category_matches_page(self):
        row = {"page_name": "Black demon", "name": "Black demon", "id": ["7874"],
               "attribute": ["demon"], "combat_level": 188}
        index, _ = make_index([row], [row])
        self.assertFalse(index["7874"]["boss"])


if __name__ == "__main__":
    unittest.main()
